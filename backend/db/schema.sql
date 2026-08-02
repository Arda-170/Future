-- =====================================================================
-- TEKNOFEST — Bağımlılıklarla Mücadelede Teknolojik Uygulamalar
-- Backend veritabanı şeması  (PostgreSQL / Supabase)
--
-- Kullanım: Supabase Dashboard > SQL Editor > yapıştır > Run
-- Tek seferde çalışır, tekrar çalıştırılabilir (idempotent).
-- =====================================================================


-- ---------------------------------------------------------------------
-- 0. Uzantılar
-- ---------------------------------------------------------------------
create extension if not exists "uuid-ossp";
create extension if not exists "pgcrypto";


-- ---------------------------------------------------------------------
-- 1. Enum tipleri
-- ---------------------------------------------------------------------
do $$ begin
  create type metric_type as enum (
    'heart_rate',        -- bpm
    'resting_heart_rate',
    'hrv',               -- ms (SDNN/RMSSD)
    'sleep_session',     -- dakika
    'sleep_stage',
    'stress',            -- 0-100 Samsung skoru
    'spo2',              -- %
    'steps',
    'screen_time',       -- dakika (telefondan, UsageStatsManager)
    'app_open'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type consent_type as enum (
    'health_data',       -- özel nitelikli kişisel veri — KVKK md.6
    'location',
    'emergency_contact',
    'notifications'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type geofence_event_type as enum ('enter', 'exit', 'dwell');
exception when duplicate_object then null; end $$;

do $$ begin
  create type risk_level as enum ('low', 'moderate', 'high');
exception when duplicate_object then null; end $$;

-- Eskalasyon durum makinesi:
--   pending -> checkin_sent -> (user_ok | user_struggling | no_response)
--                                   |            |              |
--                              resolved    support_offered  contact_notified
do $$ begin
  create type escalation_state as enum (
    'pending',
    'checkin_sent',
    'user_ok',
    'user_struggling',
    'support_offered',
    'contact_notified',
    'resolved',
    'expired'
  );
exception when duplicate_object then null; end $$;


-- ---------------------------------------------------------------------
-- 2. Yardımcı: updated_at otomatik güncelleme
-- ---------------------------------------------------------------------
create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end $$;


-- ---------------------------------------------------------------------
-- 3. Kullanıcı profili
--    Kimlik doğrulama auth.users'ta; burada sadece uygulama verisi.
--    Kişisel veri minimumda tutuluyor (KVKK — veri minimizasyonu).
-- ---------------------------------------------------------------------
create table if not exists public.profiles (
  id            uuid primary key references auth.users(id) on delete cascade,
  display_name  text,
  timezone      text not null default 'Europe/Istanbul',
  onboarded_at  timestamptz,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);

drop trigger if exists trg_profiles_touch on public.profiles;
create trigger trg_profiles_touch before update on public.profiles
  for each row execute function public.touch_updated_at();


-- ---------------------------------------------------------------------
-- 4. Rıza kayıtları (KVKK açık rıza — versiyonlu)
--    Jürinin en çok soracağı tablo. Rıza geri çekilebilir olmalı,
--    o yüzden satır silinmiyor, revoked_at doldurulUyor.
-- ---------------------------------------------------------------------
create table if not exists public.consents (
  id            uuid primary key default gen_random_uuid(),
  user_id       uuid not null references auth.users(id) on delete cascade,
  kind          consent_type not null,
  text_version  text not null,          -- ör. 'saglik-verisi-v1.2'
  granted_at    timestamptz not null default now(),
  revoked_at    timestamptz
);

create index if not exists idx_consents_user
  on public.consents (user_id, kind, granted_at desc);

-- Aktif rıza var mı? — servis katmanı her veri yazımından önce sorar.
create or replace function public.has_active_consent(p_user uuid, p_kind consent_type)
returns boolean language sql stable as $$
  select exists (
    select 1 from public.consents
    where user_id = p_user and kind = p_kind and revoked_at is null
  );
$$;


-- ---------------------------------------------------------------------
-- 5. Ham sağlık verisi
--    client_uid: telefonun ürettiği tekil anahtar. Offline kuyruktan
--    aynı kayıt iki kez gelirse sessizce yok sayılır (idempotent sync).
-- ---------------------------------------------------------------------
create table if not exists public.health_samples (
  id          bigserial primary key,
  user_id     uuid not null references auth.users(id) on delete cascade,
  client_uid  text not null,
  metric      metric_type not null,
  value       double precision not null,
  unit        text,
  start_time  timestamptz not null,
  end_time    timestamptz,
  source      text default 'health_connect',
  created_at  timestamptz not null default now(),
  unique (user_id, client_uid)
);

create index if not exists idx_samples_lookup
  on public.health_samples (user_id, metric, start_time desc);


-- ---------------------------------------------------------------------
-- 6. Günlük özet — risk skoru bunun üzerinden hesaplanır
--    Ham veriyi 90 gün sonra silsen bile bu tablo kalır.
-- ---------------------------------------------------------------------
create table if not exists public.daily_aggregates (
  user_id            uuid not null references auth.users(id) on delete cascade,
  day                date not null,
  sleep_minutes      integer,
  sleep_efficiency   numeric(5,2),
  avg_hrv            numeric(6,2),
  resting_hr         numeric(5,2),
  avg_stress         numeric(5,2),
  step_count         integer,
  screen_time_min    integer,
  night_screen_min   integer,        -- 00:00-06:00 arası — güçlü sinyal
  updated_at         timestamptz not null default now(),
  primary key (user_id, day)
);

drop trigger if exists trg_daily_touch on public.daily_aggregates;
create trigger trg_daily_touch before update on public.daily_aggregates
  for each row execute function public.touch_updated_at();


-- ---------------------------------------------------------------------
-- 7. Risk skorları
--    factors jsonb ŞART: "neden riskli dedin?" sorusunun cevabı.
--    ör. {"sleep_drop": -1.8, "hrv_drop": -2.1, "night_screen": 1.4}
-- ---------------------------------------------------------------------
create table if not exists public.risk_scores (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users(id) on delete cascade,
  computed_at  timestamptz not null default now(),
  for_day      date not null,
  score        integer not null check (score between 0 and 100),
  level        risk_level not null,
  factors      jsonb not null default '{}'::jsonb,
  model_version text not null default 'rule-v1'
);

create index if not exists idx_risk_user_time
  on public.risk_scores (user_id, computed_at desc);


-- ---------------------------------------------------------------------
-- 8. Riskli lokasyonlar
--    Mahremiyet notu: koordinat sadece zone tanımında tutulur.
--    Olay kaydında ham konum YOK — sadece hangi zone'a girildiği.
--    Bu, raporda yazacağın en güçlü mahremiyet argümanlarından biri.
-- ---------------------------------------------------------------------
create table if not exists public.geofence_zones (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  label       text not null,
  latitude    double precision not null,
  longitude   double precision not null,
  radius_m    integer not null default 150 check (radius_m between 50 and 2000),
  is_active   boolean not null default true,
  created_at  timestamptz not null default now()
);

create table if not exists public.geofence_events (
  id          bigserial primary key,
  user_id     uuid not null references auth.users(id) on delete cascade,
  zone_id     uuid not null references public.geofence_zones(id) on delete cascade,
  event_type  geofence_event_type not null,
  dwell_sec   integer,
  occurred_at timestamptz not null,
  created_at  timestamptz not null default now()
);

create index if not exists idx_geo_events_user
  on public.geofence_events (user_id, occurred_at desc);


-- ---------------------------------------------------------------------
-- 9. Ödül sistemi (contingency management)
--    Puan tek bir sayı olarak DEĞİL, ledger olarak tutuluyor.
--    Her hareket denetlenebilir; hata/hile geriye dönük izlenir.
-- ---------------------------------------------------------------------
create table if not exists public.points_ledger (
  id         bigserial primary key,
  user_id    uuid not null references auth.users(id) on delete cascade,
  delta      integer not null,
  reason     text not null,          -- 'daily_checkin', 'clean_day', 'zone_avoided'
  ref_type   text,
  ref_id     text,
  created_at timestamptz not null default now()
);

create index if not exists idx_points_user
  on public.points_ledger (user_id, created_at desc);

create or replace view public.v_points_balance as
  select user_id, coalesce(sum(delta), 0)::integer as balance
  from public.points_ledger
  group by user_id;

create table if not exists public.badges (
  code        text primary key,
  name        text not null,
  description text,
  icon        text,
  threshold   integer
);

create table if not exists public.user_badges (
  user_id    uuid not null references auth.users(id) on delete cascade,
  badge_code text not null references public.badges(code) on delete cascade,
  earned_at  timestamptz not null default now(),
  primary key (user_id, badge_code)
);


-- ---------------------------------------------------------------------
-- 10. Acil kişiler
-- ---------------------------------------------------------------------
create table if not exists public.emergency_contacts (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  name        text not null,
  phone       text not null,
  relation    text,
  priority    smallint not null default 1,
  verified_at timestamptz,            -- doğrulanmamış kişiye bildirim gitmez
  created_at  timestamptz not null default now()
);


-- ---------------------------------------------------------------------
-- 11. Eskalasyon olayları — durum makinesi burada yaşar
--     Otomatik 112 araması YOK. Bilinçli tasarım kararı, raporda yaz.
-- ---------------------------------------------------------------------
create table if not exists public.escalation_events (
  id                   uuid primary key default gen_random_uuid(),
  user_id              uuid not null references auth.users(id) on delete cascade,
  risk_score_id        uuid references public.risk_scores(id) on delete set null,
  trigger_source       text not null,   -- 'risk_score' | 'geofence' | 'manual'
  state                escalation_state not null default 'pending',
  checkin_sent_at      timestamptz,
  responded_at         timestamptz,
  contact_notified_at  timestamptz,
  notified_contact_id  uuid references public.emergency_contacts(id) on delete set null,
  timeout_at           timestamptz,     -- cron bu alana bakar
  created_at           timestamptz not null default now(),
  updated_at           timestamptz not null default now()
);

drop trigger if exists trg_esc_touch on public.escalation_events;
create trigger trg_esc_touch before update on public.escalation_events
  for each row execute function public.touch_updated_at();

create index if not exists idx_esc_pending
  on public.escalation_events (timeout_at)
  where state = 'checkin_sent';


-- ---------------------------------------------------------------------
-- 12. Denetim kaydı (KVKK — "kim hangi veriye erişti")
--     Kullanıcı kendi kaydını okuyabilir, kimse yazamaz/silemez.
-- ---------------------------------------------------------------------
create table if not exists public.audit_log (
  id         bigserial primary key,
  actor_id   uuid,
  action     text not null,
  target     text,
  target_id  text,
  meta       jsonb,
  at         timestamptz not null default now()
);


-- =====================================================================
-- 13. ROW LEVEL SECURITY
--     Her kullanıcı YALNIZCA kendi verisini görür. Uygulama katmanında
--     hata yapılsa bile veri sızmaz — koruma veritabanı seviyesinde.
-- =====================================================================

alter table public.profiles           enable row level security;
alter table public.consents           enable row level security;
alter table public.health_samples     enable row level security;
alter table public.daily_aggregates   enable row level security;
alter table public.risk_scores        enable row level security;
alter table public.geofence_zones     enable row level security;
alter table public.geofence_events    enable row level security;
alter table public.points_ledger      enable row level security;
alter table public.user_badges        enable row level security;
alter table public.emergency_contacts enable row level security;
alter table public.escalation_events  enable row level security;
alter table public.audit_log          enable row level security;
alter table public.badges             enable row level security;

-- Sahiplik bazlı tam erişim (okuma + yazma)
do $$
declare t text;
begin
  foreach t in array array[
    'profiles', 'consents', 'health_samples', 'geofence_zones',
    'geofence_events', 'emergency_contacts'
  ] loop
    execute format('drop policy if exists own_all on public.%I', t);
    execute format(
      'create policy own_all on public.%I for all
         using (auth.uid() = %s) with check (auth.uid() = %s)',
      t,
      case when t = 'profiles' then 'id' else 'user_id' end,
      case when t = 'profiles' then 'id' else 'user_id' end
    );
  end loop;
end $$;

-- Sadece okuma — bu tabloları yalnızca sunucu (service_role) yazar.
-- Kullanıcının kendi risk skorunu değiştirmesi mümkün olmamalı.
do $$
declare t text;
begin
  foreach t in array array[
    'daily_aggregates', 'risk_scores', 'points_ledger',
    'user_badges', 'escalation_events', 'audit_log'
  ] loop
    execute format('drop policy if exists own_read on public.%I', t);
    execute format(
      'create policy own_read on public.%I for select using (auth.uid() = %s)',
      t,
      case when t = 'audit_log' then 'actor_id' else 'user_id' end
    );
  end loop;
end $$;

-- Rozet kataloğu herkese açık (kişisel veri değil)
drop policy if exists badges_read on public.badges;
create policy badges_read on public.badges for select to authenticated using (true);


-- =====================================================================
-- 14. GRANT'LER
--     ÖNEMLİ: 30 Mayıs 2026 sonrası açılan Supabase projelerinde
--     PostgREST erişimi için açık grant vermek zorunlu. Bu blok
--     atlanırsa tablolar API'den görünmez.
-- =====================================================================
grant usage on schema public to anon, authenticated;

grant select, insert, update, delete on
  public.profiles, public.consents, public.health_samples,
  public.geofence_zones, public.geofence_events, public.emergency_contacts
  to authenticated;

grant select on
  public.daily_aggregates, public.risk_scores, public.points_ledger,
  public.user_badges, public.escalation_events, public.audit_log,
  public.badges, public.v_points_balance
  to authenticated;

grant usage, select on all sequences in schema public to authenticated;


-- =====================================================================
-- 15. Başlangıç verisi — rozet kataloğu
-- =====================================================================
insert into public.badges (code, name, description, threshold) values
  ('first_step',   'İlk Adım',      'Uygulamayı kurdun ve ilk verini gönderdin', 0),
  ('week_streak',  'Bir Hafta',     '7 gün üst üste günlük kontrolü tamamladın', 7),
  ('month_streak', 'Bir Ay',        '30 gün üst üste günlük kontrolü tamamladın', 30),
  ('good_sleep',   'Dinlenmiş',     '5 gün üst üste 7 saatten fazla uyudun', 5),
  ('zone_avoided', 'Yön Değiştirdi','Riskli bölgeye girmeden bir hafta geçirdin', 7),
  ('reached_out',  'Yalnız Değil',  'Zor bir anda destek istedin', 1)
on conflict (code) do nothing;


-- =====================================================================
-- 16. Cihaz token'ları (FCM push bildirimi)
-- =====================================================================
create table if not exists public.device_tokens (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users(id) on delete cascade,
  fcm_token    text not null,
  platform     text not null default 'android',
  last_seen_at timestamptz not null default now(),
  created_at   timestamptz not null default now(),
  unique (fcm_token)
);

create index if not exists idx_device_tokens_user
  on public.device_tokens (user_id);

alter table public.device_tokens enable row level security;

-- Kullanıcı sadece kendi cihazlarını görebilir.
-- Yazma yalnızca Edge Function üzerinden (service_role) yapılır.
drop policy if exists own_read on public.device_tokens;
create policy own_read on public.device_tokens
  for select using (auth.uid() = user_id);

grant select on public.device_tokens to authenticated;


-- =====================================================================
-- 17. View güvenliği
--     PostgreSQL'de view'lar RLS'i miras almaz; oluşturan kullanıcının
--     yetkisiyle çalışırlar. security_invoker açıldığında view'ı
--     sorgulayan kişinin yetkisiyle çalışır ve alttaki tablonun
--     RLS politikası devreye girer.
-- =====================================================================
alter view public.v_points_balance set (security_invoker = on);

create table if not exists public.contact_verifications (
  id          uuid primary key default gen_random_uuid(),
  contact_id  uuid not null references public.emergency_contacts(id) on delete cascade,
  code        text not null,
  attempts    int  not null default 0,
  expires_at  timestamptz not null,
  used_at     timestamptz,
  created_at  timestamptz not null default now()
);

create index if not exists idx_contact_verif
  on public.contact_verifications (contact_id, created_at desc);

-- Tamamen sunucu tarafı: RLS açık, politika yok = istemci hiçbir şey göremez.
-- Doğrulama kodları asla istemciye sızmamalı.
alter table public.contact_verifications enable row level security;

alter table public.emergency_contacts alter column user_id set default auth.uid();
alter table public.geofence_zones     alter column user_id set default auth.uid();
alter table public.geofence_events    alter column user_id set default auth.uid();
alter table public.health_samples     alter column user_id set default auth.uid();
alter table public.consents           alter column user_id set default auth.uid();