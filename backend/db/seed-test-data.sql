-- =====================================================================
-- TEST VERİSİ ÜRETİCİ
--
-- 20 günlük "normal" veri + son güne kasıtlı anomali.
-- Amaç: risk motorunun anomaliyi yakaladığını doğrulamak.
--
-- KULLANIM:
--   1. Aşağıdaki v_user değerini kendi test kullanıcının id'siyle değiştir
--      (Authentication > Users sayfasından kopyala)
--   2. SQL Editor'de çalıştır
--
-- Tekrar çalıştırılabilir — önce eski test verisini temizler.
-- =====================================================================

do $$
declare
  -- ⬇️ BURAYI DEĞİŞTİR
  v_user uuid := '1b8b87b6-5ec4-47a6-aa20-b33a42e9f155';

  v_day        date;
  v_today      date := current_date;
  v_start      date := current_date - 20;
  v_is_anomaly boolean;

  -- Kullanıcının "gerçek" normali — bunlara gürültü eklenecek
  c_sleep_base    numeric := 430;   -- ~7 saat 10 dk
  c_hrv_base      numeric := 55;    -- ms
  c_rhr_base      numeric := 62;    -- bpm
  c_night_base    numeric := 12;    -- dk, gece ekran

  v_sleep   numeric;
  v_hrv     numeric;
  v_rhr     numeric;
  v_night   numeric;
  v_steps   numeric;
  v_screen  numeric;
begin

  -- -------------------------------------------------------------------
  -- 0. Ön koşullar: profil + rıza
  -- -------------------------------------------------------------------
  insert into public.profiles (id, display_name, timezone, onboarded_at)
  values (v_user, 'Test Kullanıcı', 'Europe/Istanbul', now())
  on conflict (id) do nothing;

  insert into public.consents (user_id, kind, text_version)
  select v_user, 'health_data', 'saglik-verisi-v1.0'
  where not exists (
    select 1 from public.consents
    where user_id = v_user and kind = 'health_data' and revoked_at is null
  );

  -- -------------------------------------------------------------------
  -- 1. Eski test verisini temizle
  -- -------------------------------------------------------------------
  delete from public.health_samples   where user_id = v_user and source = 'seed';
  delete from public.daily_aggregates where user_id = v_user;
  delete from public.risk_scores      where user_id = v_user;

  -- -------------------------------------------------------------------
  -- 2. Gün gün veri üret
  -- -------------------------------------------------------------------
  v_day := v_start;

  while v_day <= v_today loop

    v_is_anomaly := (v_day = v_today);

    if v_is_anomaly then
      -- Son gün: her şey riskli yöne kayıyor
      v_sleep  := c_sleep_base - 150;              -- ~2.5 saat eksik uyku
      v_hrv    := c_hrv_base   - 18;               -- belirgin HRV düşüşü
      v_rhr    := c_rhr_base   + 11;               -- nabız yükselmiş
      v_night  := c_night_base + 75;               -- gece ekranı patlamış
      v_steps  := 2100;
      v_screen := 410;
    else
      -- Normal günler: temel çizgi + küçük rastgele gürültü
      v_sleep  := c_sleep_base + (random() * 50 - 25);
      v_hrv    := c_hrv_base   + (random() * 8  - 4);
      v_rhr    := c_rhr_base   + (random() * 4  - 2);
      v_night  := greatest(0, c_night_base + (random() * 10 - 5));
      v_steps  := 6000 + random() * 3000;
      v_screen := 200  + random() * 60;
    end if;

    -- --- Uyku (tek oturum, gece yarısı civarı) ------------------------
    insert into public.health_samples
      (user_id, client_uid, metric, value, unit, start_time, source)
    values
      (v_user, 'seed-sleep-' || v_day, 'sleep_session', round(v_sleep),
       'min', (v_day::timestamp + interval '3 hours') at time zone 'Europe/Istanbul', 'seed');

    -- --- HRV ----------------------------------------------------------
    insert into public.health_samples
      (user_id, client_uid, metric, value, unit, start_time, source)
    values
      (v_user, 'seed-hrv-' || v_day, 'hrv', round(v_hrv, 1),
       'ms', (v_day::timestamp + interval '5 hours') at time zone 'Europe/Istanbul', 'seed');

    -- --- Dinlenme nabzı -----------------------------------------------
    insert into public.health_samples
      (user_id, client_uid, metric, value, unit, start_time, source)
    values
      (v_user, 'seed-rhr-' || v_day, 'resting_heart_rate', round(v_rhr, 1),
       'bpm', (v_day::timestamp + interval '5 hours') at time zone 'Europe/Istanbul', 'seed');

    -- --- Adım ----------------------------------------------------------
    insert into public.health_samples
      (user_id, client_uid, metric, value, unit, start_time, source)
    values
      (v_user, 'seed-steps-' || v_day, 'steps', round(v_steps),
       'count', (v_day::timestamp + interval '14 hours') at time zone 'Europe/Istanbul', 'seed');

    -- --- Gündüz ekran süresi (saat 14, gece penceresinin dışında) -------
    insert into public.health_samples
      (user_id, client_uid, metric, value, unit, start_time, source)
    values
      (v_user, 'seed-screen-day-' || v_day, 'screen_time', round(v_screen - v_night),
       'min', (v_day::timestamp + interval '14 hours') at time zone 'Europe/Istanbul', 'seed');

    -- --- Gece ekran süresi (saat 02, gece penceresi içinde) -------------
    insert into public.health_samples
      (user_id, client_uid, metric, value, unit, start_time, source)
    values
      (v_user, 'seed-screen-night-' || v_day, 'screen_time', round(v_night),
       'min', (v_day::timestamp + interval '2 hours') at time zone 'Europe/Istanbul', 'seed');

    -- --- Günlük özeti hemen üret ---------------------------------------
    perform public.build_daily_aggregate(v_user, v_day);

    v_day := v_day + 1;
  end loop;

  raise notice 'Test verisi üretildi: % gün', (v_today - v_start + 1);
end $$;


-- =====================================================================
-- DOĞRULAMA SORGULARI
-- =====================================================================

-- 1. Günlük özetler doğru oluşmuş mu? Son gün diğerlerinden sapmalı.
select day, sleep_minutes, round(avg_hrv,1) as hrv, round(resting_hr,1) as rhr,
       night_screen_min, step_count
from public.daily_aggregates
where user_id = '1b8b87b6-5ec4-47a6-aa20-b33a42e9f155'
order by day desc
limit 8;


-- 2. Temel çizgi hesaplanıyor mu? n_days 14 olmalı.
select * from public.get_baseline(
  '1b8b87b6-5ec4-47a6-aa20-b33a42e9f155'::uuid,
  current_date
);


-- 3. NORMAL BİR GÜN — skor düşük çıkmalı (low)
select public.compute_risk_score(
  '1b8b87b6-5ec4-47a6-aa20-b33a42e9f155'::uuid,
  current_date - 1
);


-- 4. ANOMALİ GÜNÜ — skor yüksek çıkmalı (high)
select jsonb_pretty(
  public.compute_risk_score(
    '1b8b87b6-5ec4-47a6-aa20-b33a42e9f155'::uuid,
    current_date
  )
);


-- 5. Yetersiz veri durumu doğru çalışıyor mu?
--    Temel çizgi penceresinin başındaki bir gün — 7 günden az veri var.
select public.compute_risk_score(
  '1b8b87b6-5ec4-47a6-aa20-b33a42e9f155'::uuid,
  current_date - 18
);
