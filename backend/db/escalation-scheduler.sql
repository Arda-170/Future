-- =====================================================================
-- ESKALASYON ZAMANLAYICISI
--
-- Check-in'e 15 dakika içinde cevap gelmezse acil kişiye bildirim.
-- Bu, eskalasyon durum makinesinin "cevap yok" dalıdır.
--
-- Kullanım: SQL Editor > yapıştır > Run
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Uzantılar
-- ---------------------------------------------------------------------
create extension if not exists pg_cron;


-- ---------------------------------------------------------------------
-- 2. Bildirim kuyruğu
--
--    Acil kişi uygulamanın kullanıcısı DEĞİL — telefonunda app yok,
--    FCM token'ı yok. Ona ulaşmanın tek yolu SMS.
--
--    SMS sağlayıcı entegrasyonu (Netgsm/İletimerkezi) sözleşme
--    gerektirdiğinden MVP'de kuyruk yapısı kuruluyor, gerçek gönderim
--    pilot fazına bırakılıyor. Kuyruktaki kayıtlar demoda gösterilir.
-- ---------------------------------------------------------------------
create table if not exists public.notification_queue (
  id             bigserial primary key,
  escalation_id  uuid references public.escalation_events(id) on delete cascade,
  channel        text not null,           -- 'sms' | 'push'
  recipient      text not null,           -- telefon numarası veya fcm token
  payload        jsonb not null,
  status         text not null default 'pending',  -- pending | sent | failed
  attempts       int  not null default 0,
  last_error     text,
  created_at     timestamptz not null default now(),
  sent_at        timestamptz
);

create index if not exists idx_notif_pending
  on public.notification_queue (status, created_at)
  where status = 'pending';

alter table public.notification_queue enable row level security;
-- Kullanıcıya açılmıyor: bu tablo tamamen sunucu tarafı.
-- RLS açık ve politika yok = authenticated rolü hiçbir satır göremez.


-- ---------------------------------------------------------------------
-- 3. Süresi dolan check-in'leri işle
--
--    expire_stale_checkins() durumu değiştirir; bu fonksiyon ayrıca
--    acil kişiyi bulup bildirim kuyruğuna kayıt düşer.
-- ---------------------------------------------------------------------
create or replace function public.process_stale_checkins()
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  r          record;
  v_contact  record;
  v_count    int := 0;
begin
  for r in
    update public.escalation_events
    set state = 'contact_notified',
        contact_notified_at = now()
    where state = 'checkin_sent'
      and timeout_at < now()
    returning id, user_id
  loop
    -- Doğrulanmış, en yüksek öncelikli acil kişi
    select id, name, phone into v_contact
    from public.emergency_contacts
    where user_id = r.user_id
      and verified_at is not null
    order by priority asc
    limit 1;

    if v_contact.id is not null then
      insert into public.notification_queue (escalation_id, channel, recipient, payload)
      values (
        r.id,
        'sms',
        v_contact.phone,
        jsonb_build_object(
          'template', 'emergency_checkin_no_response',
          'contact_name', v_contact.name,
          -- Mesajda tanı, madde adı veya "bağımlılık" kelimesi GEÇMEZ.
          -- Acil kişi kullanıcının durumunu bilmiyor olabilir; mahremiyet.
          'text', v_contact.name || ', destek olduğun kişi bugün uygulamamızdaki ' ||
                  'kontrol mesajına yanıt vermedi. Uygun bir zamanda arayıp ' ||
                  'nasıl olduğunu sorabilir misin?'
        )
      );

      update public.escalation_events
      set notified_contact_id = v_contact.id
      where id = r.id;
    else
      -- Acil kişi yoksa kayıt yine de tutulur; kullanıcıya uygulama
      -- içinde destek kaynakları gösterilir.
      insert into public.notification_queue (escalation_id, channel, recipient, payload)
      values (
        r.id, 'push', r.user_id::text,
        jsonb_build_object(
          'template', 'no_contact_fallback',
          'title', 'Buradayız',
          'text', 'Zorlanıyorsan YEDAM Danışma Hattı 115''i arayabilirsin.'
        )
      );
    end if;

    v_count := v_count + 1;
  end loop;

  return v_count;
end $$;


-- ---------------------------------------------------------------------
-- 4. Zamanlayıcı işleri
-- ---------------------------------------------------------------------

-- Var olan işleri temizle (tekrar çalıştırılabilirlik)
select cron.unschedule(jobname)
from cron.job
where jobname in ('process-stale-checkins', 'nightly-risk-compute');

-- Her dakika: süresi dolmuş check-in'leri işle
select cron.schedule(
  'process-stale-checkins',
  '* * * * *',
  $$ select public.process_stale_checkins(); $$
);

-- Her gece 03:00 (UTC): tüm kullanıcılar için dünün risk skorunu hesapla
-- Saat 03:00 seçildi çünkü uyku verisi o saatte tamamlanmış olur.
select cron.schedule(
  'nightly-risk-compute',
  '0 3 * * *',
  $$
    do $inner$
    declare u record;
    begin
      for u in
        select p.id
        from public.profiles p
        where public.has_active_consent(p.id, 'health_data')
      loop
        perform public.build_daily_aggregate(u.id, current_date - 1);
        perform public.compute_risk_score(u.id, current_date - 1);
      end loop;
    end $inner$;
  $$
);


-- ---------------------------------------------------------------------
-- 5. Yetkiler
-- ---------------------------------------------------------------------
revoke execute on function public.process_stale_checkins() from public, anon, authenticated;
grant  execute on function public.process_stale_checkins() to service_role;


-- =====================================================================
-- DOĞRULAMA
-- =====================================================================

-- Zamanlanmış işler görünüyor mu?
select jobid, jobname, schedule, active from cron.job;

-- Son çalıştırmalar (birkaç dakika bekledikten sonra bak)
select jobid, status, return_message, start_time
from cron.job_run_details
order by start_time desc
limit 10;