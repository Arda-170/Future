-- =====================================================================
-- ROZET OTOMASYONU + GEOFENCE TETİKLEYİCİ
--
-- Kullanım: SQL Editor > yapıştır > Run
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Seri (streak) hesaplama
--    Kullanıcının kaç gün üst üste check-in tamamladığı.
--    Bugünden geriye doğru kesintisiz gün sayısı.
-- ---------------------------------------------------------------------
create or replace function public.get_checkin_streak(p_user uuid)
returns int
language sql
stable
security definer
set search_path = public
as $$
  with days as (
    select distinct created_at::date as d
    from public.points_ledger
    where user_id = p_user
      and reason in ('daily_checkin', 'reached_out')
  ),
  numbered as (
    select d, row_number() over (order by d desc) as rn
    from days
    where d <= current_date
  )
  select coalesce(count(*), 0)::int
  from numbered
  where d = current_date - (rn - 1)::int;
$$;


-- ---------------------------------------------------------------------
-- 2. Rozet değerlendirme
--    Her puan hareketinden sonra çalışır; hak edilen rozetleri verir.
--    Zaten kazanılmış rozetler tekrar verilmez (primary key koruması).
-- ---------------------------------------------------------------------
create or replace function public.evaluate_badges(p_user uuid)
returns text[]
language plpgsql
security definer
set search_path = public
as $$
declare
  v_earned    text[] := '{}';
  v_streak    int;
  v_sleep_run int;
  v_clean_zone int;
begin
  -- --- İlk Adım: ilk veri gönderimi ---------------------------------
  if exists (select 1 from public.health_samples where user_id = p_user limit 1) then
    insert into public.user_badges (user_id, badge_code)
    values (p_user, 'first_step')
    on conflict do nothing;
    if found then v_earned := v_earned || 'first_step'; end if;
  end if;

  -- --- Seri rozetleri ------------------------------------------------
  v_streak := public.get_checkin_streak(p_user);

  if v_streak >= 7 then
    insert into public.user_badges (user_id, badge_code)
    values (p_user, 'week_streak') on conflict do nothing;
    if found then v_earned := v_earned || 'week_streak'; end if;
  end if;

  if v_streak >= 30 then
    insert into public.user_badges (user_id, badge_code)
    values (p_user, 'month_streak') on conflict do nothing;
    if found then v_earned := v_earned || 'month_streak'; end if;
  end if;

  -- --- Dinlenmiş: 5 gün üst üste 7+ saat uyku ------------------------
  select count(*) into v_sleep_run
  from (
    select day, sleep_minutes,
           day - (row_number() over (order by day))::int as grp
    from public.daily_aggregates
    where user_id = p_user
      and sleep_minutes >= 420
      and day > current_date - 14
  ) t
  group by grp
  order by count(*) desc
  limit 1;

  if coalesce(v_sleep_run, 0) >= 5 then
    insert into public.user_badges (user_id, badge_code)
    values (p_user, 'good_sleep') on conflict do nothing;
    if found then v_earned := v_earned || 'good_sleep'; end if;
  end if;

  -- --- Yön Değiştirdi: 7 gün riskli bölgeye girilmedi ----------------
  -- Sadece en az bir zone tanımlamış kullanıcılar için anlamlı.
  if exists (
    select 1 from public.geofence_zones
    where user_id = p_user and is_active
  ) then
    select count(*) into v_clean_zone
    from public.geofence_events
    where user_id = p_user
      and event_type = 'enter'
      and occurred_at > now() - interval '7 days';

    if v_clean_zone = 0 then
      insert into public.user_badges (user_id, badge_code)
      values (p_user, 'zone_avoided') on conflict do nothing;
      if found then v_earned := v_earned || 'zone_avoided'; end if;
    end if;
  end if;

  -- --- Yalnız Değil: destek istedi -----------------------------------
  if exists (
    select 1 from public.points_ledger
    where user_id = p_user and reason = 'reached_out'
  ) then
    insert into public.user_badges (user_id, badge_code)
    values (p_user, 'reached_out') on conflict do nothing;
    if found then v_earned := v_earned || 'reached_out'; end if;
  end if;

  -- Kazanılan rozetler için bildirim kuyruğuna kayıt
  if array_length(v_earned, 1) > 0 then
    insert into public.notification_queue (channel, recipient, payload)
    select 'push', p_user::text, jsonb_build_object(
      'type', 'badge_earned',
      'badge_code', b,
      'title', 'Yeni rozet!',
      'text', (select name from public.badges where code = b)
    )
    from unnest(v_earned) as b;
  end if;

  return v_earned;
end $$;


-- ---------------------------------------------------------------------
-- 3. Puan hareketinden sonra rozetleri otomatik değerlendir
-- ---------------------------------------------------------------------
create or replace function public.trg_points_evaluate_badges()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.evaluate_badges(new.user_id);
  return new;
end $$;

drop trigger if exists after_points_insert on public.points_ledger;
create trigger after_points_insert
  after insert on public.points_ledger
  for each row execute function public.trg_points_evaluate_badges();


-- =====================================================================
-- GEOFENCE TETİKLEYİCİ
--
-- Karar hatırlatması: geofence risk SKORUNU etkilemez, ayrı bir
-- tetikleyicidir. Fizyolojik risk yavaş birikir, konum anlıktır;
-- ikisini tek sayıda karıştırmak ikisini de bulanıklaştırır.
--
-- Davranış: riskli bölgeye giriş -> destekleyici mesaj (suçlayıcı değil).
-- Eskalasyon BAŞLATMAZ — sadece bölgeye girmek kriz demek değildir.
-- =====================================================================

create or replace function public.trg_geofence_enter()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_label      text;
  v_recent     int;
  v_risk_level risk_level;
begin
  if new.event_type <> 'enter' then
    return new;
  end if;

  -- Soğuma: aynı bölge için 6 saatte bir mesaj.
  -- Kullanıcı evinin yakınındaki bir bölgeye günde 5 kez girip
  -- çıkabilir; her seferinde bildirim göndermek rahatsız edici olur.
  select count(*) into v_recent
  from public.geofence_events
  where user_id = new.user_id
    and zone_id = new.zone_id
    and event_type = 'enter'
    and occurred_at > now() - interval '6 hours'
    and id <> new.id;

  if v_recent > 0 then
    return new;
  end if;

  select label into v_label
  from public.geofence_zones where id = new.zone_id;

  -- Son risk skoru yüksekse mesaj daha destekleyici olsun
  select level into v_risk_level
  from public.risk_scores
  where user_id = new.user_id
  order by computed_at desc
  limit 1;

  insert into public.notification_queue (channel, recipient, payload)
  values (
    'push',
    new.user_id::text,
    jsonb_build_object(
      'type', 'zone_enter',
      'zone_label', coalesce(v_label, 'bir bölge'),
      'title', 'Buradayız',
      -- Dil önemli: "riskli bölgedesin" DEĞİL. Suçlama yok, uyarı yok.
      'text', case
        when v_risk_level = 'high' then
          'Kendini zorlanmış hissediyorsan bir dakika durup nefes al. ' ||
          'İstersen YEDAM Danışma Hattı 115''i arayabilirsin.'
        else
          'Kendine dikkat et. Bugün nasıl gittiğini uygulamadan görebilirsin.'
      end
    )
  );

  return new;
end $$;

drop trigger if exists after_geofence_enter on public.geofence_events;
create trigger after_geofence_enter
  after insert on public.geofence_events
  for each row execute function public.trg_geofence_enter();


-- ---------------------------------------------------------------------
-- 4. "Temiz gün" puanı — her gece
--    Riskli bölgeye girilmeden geçen gün ödüllendirilir.
-- ---------------------------------------------------------------------
create or replace function public.award_clean_days()
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  u      record;
  v_count int := 0;
begin
  for u in
    select p.id
    from public.profiles p
    where exists (
      select 1 from public.geofence_zones z
      where z.user_id = p.id and z.is_active
    )
  loop
    if not exists (
      select 1 from public.geofence_events
      where user_id = u.id
        and event_type = 'enter'
        and occurred_at::date = current_date - 1
    ) then
      perform public.award_points(u.id, 5, 'clean_day',
                                  (current_date - 1)::text);
      v_count := v_count + 1;
    end if;
  end loop;

  return v_count;
end $$;

select cron.unschedule(jobname)
from cron.job where jobname = 'nightly-clean-days';

select cron.schedule(
  'nightly-clean-days',
  '30 3 * * *',
  $$ select public.award_clean_days(); $$
);


-- ---------------------------------------------------------------------
-- 5. Yetkiler
-- ---------------------------------------------------------------------
revoke execute on function public.evaluate_badges(uuid)      from public, anon, authenticated;
revoke execute on function public.award_clean_days()         from public, anon, authenticated;
grant  execute on function public.get_checkin_streak(uuid)   to authenticated;  -- UI seriyi gösterebilsin
grant  execute on function public.evaluate_badges(uuid)      to service_role;
grant  execute on function public.award_clean_days()         to service_role;