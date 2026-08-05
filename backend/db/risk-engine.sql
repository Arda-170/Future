-- =====================================================================
-- RİSK MOTORU — SQL KATMANI
--
-- Yaklaşım: kişiye özel sapma (z-skoru), mutlak eşik DEĞİL.
-- Her kullanıcının kendi 14 günlük temel çizgisi hesaplanır; bugünkü
-- değerin kendi normalinden kaç standart sapma uzaklaştığına bakılır.
--
-- Kullanım: SQL Editor'e yapıştır > Run
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Günlük özet üretimi
--    Ham health_samples verisini daily_aggregates'e indirger.
--    Gece ekran süresi 00:00-06:00 arası ayrı hesaplanır.
-- ---------------------------------------------------------------------
create or replace function public.build_daily_aggregate(
  p_user uuid,
  p_day  date
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_tz text;
begin
  select coalesce(timezone, 'Europe/Istanbul') into v_tz
  from public.profiles where id = p_user;

  insert into public.daily_aggregates as d (
    user_id, day,
    sleep_minutes, sleep_efficiency, avg_hrv, resting_hr,
    avg_stress, step_count, screen_time_min, night_screen_min
  )
  select
    p_user,
    p_day,
    -- Uyku: sleep_session örneklerinin toplamı (dakika)
    nullif(sum(value) filter (where metric = 'sleep_session'), 0)::int,
    -- Uyku verimliliği: yatakta geçen süreye oranı — veri yoksa null
    null::numeric,
    avg(value) filter (where metric = 'hrv'),
    avg(value) filter (where metric = 'resting_heart_rate'),
    avg(value) filter (where metric = 'stress'),
    sum(value) filter (where metric = 'steps')::int,
    sum(value) filter (where metric = 'screen_time')::int,
    -- Gece ekranı: yerel saat 00:00-06:00 arası
    sum(value) filter (
      where metric = 'screen_time'
        and extract(hour from start_time at time zone v_tz) < 6
    )::int
  from public.health_samples
  where user_id = p_user
    and (start_time at time zone v_tz)::date = p_day
  on conflict (user_id, day) do update set
    sleep_minutes    = excluded.sleep_minutes,
    sleep_efficiency = excluded.sleep_efficiency,
    avg_hrv          = excluded.avg_hrv,
    resting_hr       = excluded.resting_hr,
    avg_stress       = excluded.avg_stress,
    step_count       = excluded.step_count,
    screen_time_min  = excluded.screen_time_min,
    night_screen_min = excluded.night_screen_min,
    updated_at       = now();
end $$;


-- ---------------------------------------------------------------------
-- 2. Kişisel temel çizgi
--    Hedef günden ÖNCEKİ 14 gün. Hedef gün dahil edilmez —
--    yoksa anomali kendi temel çizgisini yukarı çeker.
-- ---------------------------------------------------------------------
create or replace function public.get_baseline(
  p_user uuid,
  p_day  date,
  p_window_days int default 14
)
returns table (
  n_days           int,
  sleep_mean       numeric, sleep_sd       numeric,
  hrv_mean         numeric, hrv_sd         numeric,
  rhr_mean         numeric, rhr_sd         numeric,
  night_screen_mean numeric, night_screen_sd numeric
)
language sql
stable
security definer
set search_path = public
as $$
  select
    count(*)::int,
    avg(sleep_minutes),    nullif(stddev_samp(sleep_minutes), 0),
    avg(avg_hrv),          nullif(stddev_samp(avg_hrv), 0),
    avg(resting_hr),       nullif(stddev_samp(resting_hr), 0),
    avg(night_screen_min), nullif(stddev_samp(night_screen_min), 0)
  from public.daily_aggregates
  where user_id = p_user
    and day <  p_day
    and day >= p_day - p_window_days;
$$;


-- ---------------------------------------------------------------------
-- 3. Risk skoru hesaplama
--
--    Ağırlıklar:
--      uyku düşüşü        0.30
--      HRV düşüşü         0.25
--      dinlenme nabzı ↑   0.20
--      gece ekranı ↑      0.25
--
--    Her sinyal z-skoruna çevrilir, yönü riskli tarafa normalize edilir
--    (uyku ve HRV için düşüş riskli, nabız ve ekran için artış riskli),
--    0-3 aralığına kırpılır, ağırlıkla çarpılır.
--
--    Eksik sinyal varsa ağırlıklar kalanlar arasında yeniden dağıtılır —
--    böylece "veri gelmedi" durumu yapay olarak düşük skor üretmez.
-- ---------------------------------------------------------------------
create or replace function public.compute_risk_score(
  p_user uuid,
  p_day  date
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  b            record;
  t            record;
  v_factors    jsonb := '{}'::jsonb;
  v_weight_sum numeric := 0;
  v_score_acc  numeric := 0;
  v_z          numeric;
  v_score      int;
  v_level      risk_level;

  -- Yeterli veri eşiği (kararlaştırıldı: en az 7 gün)
  c_min_days constant int := 7;

  -- ---------------------------------------------------------------------
  -- YENİ: Mutlak Güvenlik Eşiği (Absolute Safety-Net)
  -- Kaynak: Clinical Opiate Withdrawal Scale (COWS) — dinlenik haldeki
  -- nabız, kişiye özel karşılaştırma gerektirmeyen objektif bir kriz
  -- göstergesidir (120 bpm üzeri = şiddetli). Bu katman, 7 günlük
  -- baseline şartını beklemeden çalışır; ilk hafta içindeki gerçek bir
  -- krizin "insufficient_data" ile sessiz geçilmesini engeller.
  -- ---------------------------------------------------------------------
  c_abs_hr_severe  constant numeric := 120;
  c_max_steps_rest constant int     := 150;  -- "hareketsiz" sayılma sınırı
begin
  -- Günün özetini baseline şartından önce çek
  select * into t from public.daily_aggregates
  where user_id = p_user and day = p_day;

  if t.resting_hr is not null
     and coalesce(t.step_count, 0) < c_max_steps_rest
     and t.resting_hr >= c_abs_hr_severe then

    v_factors := v_factors || jsonb_build_object('abs_hr_safety_net', jsonb_build_object(
      'value', t.resting_hr,
      'threshold', c_abs_hr_severe,
      'label', 'Dinlenme nabzı klinik olarak şiddetli kriz eşiğinin üzerinde (COWS referansı)',
      'note', 'Kişisel baseline''dan bağımsız mutlak eşik'
    ));

    insert into public.risk_scores (user_id, for_day, score, level, factors, model_version)
    values (p_user, p_day, 100, 'high', v_factors, 'abs-safety-net-v1');

    return jsonb_build_object(
      'status', 'ok',
      'score', 100,
      'level', 'high',
      'factors', v_factors,
      'baseline_days', coalesce((select n_days from public.get_baseline(p_user, p_day)), 0),
      'triggered_by', 'absolute_threshold'
    );
  end if;

  select * into b from public.get_baseline(p_user, p_day);

  if b.n_days is null or b.n_days < c_min_days then
    return jsonb_build_object(
      'status', 'insufficient_data',
      'days_available', coalesce(b.n_days, 0),
      'days_required', c_min_days
    );
  end if;

  if t is null then
    return jsonb_build_object('status', 'no_data_for_day');
  end if;

  -- --- Uyku süresi: DÜŞÜŞ riskli ------------------------------------
  if t.sleep_minutes is not null and b.sleep_sd is not null then
    v_z := greatest(0, least(3, (b.sleep_mean - t.sleep_minutes) / b.sleep_sd));
    v_score_acc  := v_score_acc + v_z * 0.30;
    v_weight_sum := v_weight_sum + 0.30;
    v_factors := v_factors || jsonb_build_object('sleep_drop', jsonb_build_object(
      'z', round(v_z, 2), 'weight', 0.30,
      'value', t.sleep_minutes, 'baseline', round(b.sleep_mean, 1),
      'label', 'Uyku süresi normalinin altında'
    ));
  end if;

  -- --- HRV: DÜŞÜŞ riskli --------------------------------------------
  if t.avg_hrv is not null and b.hrv_sd is not null then
    v_z := greatest(0, least(3, (b.hrv_mean - t.avg_hrv) / b.hrv_sd));
    v_score_acc  := v_score_acc + v_z * 0.25;
    v_weight_sum := v_weight_sum + 0.25;
    v_factors := v_factors || jsonb_build_object('hrv_drop', jsonb_build_object(
      'z', round(v_z, 2), 'weight', 0.25,
      'value', round(t.avg_hrv, 1), 'baseline', round(b.hrv_mean, 1),
      'label', 'Kalp atım değişkenliği düşmüş'
    ));
  end if;

  -- --- Dinlenme nabzı: ARTIŞ riskli ---------------------------------
  if t.resting_hr is not null and b.rhr_sd is not null then
    v_z := greatest(0, least(3, (t.resting_hr - b.rhr_mean) / b.rhr_sd));
    v_score_acc  := v_score_acc + v_z * 0.20;
    v_weight_sum := v_weight_sum + 0.20;
    v_factors := v_factors || jsonb_build_object('rhr_rise', jsonb_build_object(
      'z', round(v_z, 2), 'weight', 0.20,
      'value', round(t.resting_hr, 1), 'baseline', round(b.rhr_mean, 1),
      'label', 'Dinlenme nabzı yükselmiş'
    ));
  end if;

  -- --- Gece ekran süresi: ARTIŞ riskli ------------------------------
  if t.night_screen_min is not null and b.night_screen_sd is not null then
    v_z := greatest(0, least(3, (t.night_screen_min - b.night_screen_mean) / b.night_screen_sd));
    v_score_acc  := v_score_acc + v_z * 0.25;
    v_weight_sum := v_weight_sum + 0.25;
    v_factors := v_factors || jsonb_build_object('night_screen', jsonb_build_object(
      'z', round(v_z, 2), 'weight', 0.25,
      'value', t.night_screen_min, 'baseline', round(b.night_screen_mean, 1),
      'label', 'Gece ekran süresi artmış'
    ));
  end if;

  -- En az iki sinyal olmadan skor üretme
  if v_weight_sum < 0.45 then
    return jsonb_build_object(
      'status', 'insufficient_signals',
      'weight_available', v_weight_sum
    );
  end if;

  -- Ağırlıkları yeniden normalize et, 0-100'e ölçekle.
  -- z=3 (üç standart sapma) tam 100 puana karşılık gelir.
  v_score := round(least(100, (v_score_acc / v_weight_sum) / 3.0 * 100))::int;

  v_level := case
    when v_score >= 70 then 'high'
    when v_score >= 40 then 'moderate'
    else 'low'
  end::risk_level;

  insert into public.risk_scores (user_id, for_day, score, level, factors, model_version)
  values (p_user, p_day, v_score, v_level, v_factors, 'rule-v1');

  return jsonb_build_object(
    'status', 'ok',
    'score', v_score,
    'level', v_level,
    'factors', v_factors,
    'baseline_days', b.n_days
  );
end $$;


-- ---------------------------------------------------------------------
-- 4. Eskalasyon başlatma
--    Soğuma kuralı: aynı kullanıcıya 24 saatte en fazla bir check-in.
--    Yanlış pozitifler kullanıcıyı uygulamadan soğutur.
-- ---------------------------------------------------------------------
create or replace function public.maybe_start_escalation(
  p_user     uuid,
  p_score_id uuid,
  p_source   text default 'risk_score'
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_recent  int;
  v_id      uuid;
  c_cooldown constant interval := interval '24 hours';
  c_timeout  constant interval := interval '15 minutes';
begin
  select count(*) into v_recent
  from public.escalation_events
  where user_id = p_user
    and created_at > now() - c_cooldown
    and state <> 'expired';

  if v_recent > 0 then
    return null;   -- soğuma süresi dolmadı
  end if;

  insert into public.escalation_events (
    user_id, risk_score_id, trigger_source, state, timeout_at
  )
  values (
    p_user, p_score_id, p_source, 'pending', now() + c_timeout
  )
  returning id into v_id;

  return v_id;
end $$;


-- ---------------------------------------------------------------------
-- 5. Zaman aşımına uğrayan check-in'leri işaretle
--    pg_cron her dakika çağırır. Bildirimi Edge Function gönderir;
--    burada sadece durum değişimi yapılır.
-- ---------------------------------------------------------------------
create or replace function public.expire_stale_checkins()
returns table (escalation_id uuid, user_id uuid)
language sql
security definer
set search_path = public
as $$
  update public.escalation_events
  set state = 'contact_notified',
      contact_notified_at = now()
  where state = 'checkin_sent'
    and timeout_at < now()
  returning id, user_id;
$$;


-- ---------------------------------------------------------------------
-- 6. Puan verme — tek giriş noktası
--    Aynı gün aynı sebeple iki kez puan verilmesini engeller.
-- ---------------------------------------------------------------------
create or replace function public.award_points(
  p_user   uuid,
  p_delta  int,
  p_reason text,
  p_ref_id text default null
)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_exists int;
begin
  select count(*) into v_exists
  from public.points_ledger
  where user_id = p_user
    and reason = p_reason
    and created_at::date = current_date
    and (p_ref_id is null or ref_id = p_ref_id);

  if v_exists > 0 then
    return 0;
  end if;

  insert into public.points_ledger (user_id, delta, reason, ref_id)
  values (p_user, p_delta, p_reason, p_ref_id);

  return p_delta;
end $$;


-- ---------------------------------------------------------------------
-- 7. Yetkiler
--    Bu fonksiyonlar security definer — yalnızca service_role çağırabilir.
--    İstemcinin doğrudan çağırması engellenir.
-- ---------------------------------------------------------------------
revoke execute on function public.build_daily_aggregate(uuid, date) from public, anon, authenticated;
revoke execute on function public.compute_risk_score(uuid, date)    from public, anon, authenticated;
revoke execute on function public.maybe_start_escalation(uuid, uuid, text) from public, anon, authenticated;
revoke execute on function public.expire_stale_checkins()           from public, anon, authenticated;
revoke execute on function public.award_points(uuid, int, text, text) from public, anon, authenticated;
revoke execute on function public.get_baseline(uuid, date, int)      from public, anon, authenticated;

grant execute on function public.build_daily_aggregate(uuid, date)   to service_role;
grant execute on function public.compute_risk_score(uuid, date)      to service_role;
grant execute on function public.maybe_start_escalation(uuid, uuid, text) to service_role;
grant execute on function public.expire_stale_checkins()             to service_role;
grant execute on function public.award_points(uuid, int, text, text) to service_role;
grant execute on function public.get_baseline(uuid, date, int)       to service_role;