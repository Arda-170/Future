# API Sözleşmesi — v1

TEKNOFEST Bağımlılıklarla Mücadelede Teknolojik Uygulamalar
Backend: Supabase (PostgreSQL + PostgREST + Edge Functions)

> Bu dosya mobil ve ön yüz ekibi için bağlayıcı sözleşmedir.
> Alan adları ve dönüş yapıları burada yazdığı gibidir; değişiklik olursa
> bu dosya güncellenir ve ekibe bildirilir.

---

## 0. Temel kurallar

**İki tür erişim var:**

| Tür | Ne zaman | Nasıl |
|---|---|---|
| **Doğrudan tablo** | Kullanıcının kendi verisini yazması/okuması | Supabase SDK (`supabase.from(...)`) |
| **Edge Function** | Sonucu sunucunun belirlemesi gereken işler | HTTP POST (`supabase.functions.invoke(...)`) |

**Neden bu ayrım:** Kullanıcının lehine sonuç doğuran hiçbir şey istemciden yazılamaz.
Risk skoru, puan, rozet, eskalasyon → yalnızca sunucu yazar.

**Kimlik doğrulama:** Tüm çağrılarda Supabase Auth JWT'si otomatik gider.
`auth.uid()` RLS tarafından okunur; istemci hiçbir yerde `user_id` göndermez
(gönderse bile RLS reddeder).

**Zaman formatı:** Tüm zamanlar ISO 8601 + UTC offset.
Örnek: `2026-08-01T23:14:00+03:00`

**Hata formatı (Edge Function'lar):**
```json
{ "error": { "code": "CONSENT_REQUIRED", "message": "Sağlık verisi rızası bulunamadı" } }
```

---

## 1. Onboarding akışı

Sıra önemlidir. Rıza alınmadan sağlık verisi gönderilmesi sunucuda reddedilir.

```
1. Kayıt / giriş        → supabase.auth.signUp / signInWithPassword
2. Profil oluştur       → profiles insert
3. Rıza ekranları       → consents insert  (her tip için ayrı satır)
4. Health Connect izni  → (Android tarafı, sunucuyu ilgilendirmez)
5. İlk senkronizasyon   → health_samples insert
```

### 1.1 Profil oluşturma

```kotlin
supabase.from("profiles").insert(
  mapOf(
    "id" to supabase.auth.currentUserOrNull()!!.id,
    "display_name" to "Ahmet",
    "timezone" to "Europe/Istanbul",
    "onboarded_at" to Instant.now().toString()
  )
)
```

### 1.2 Rıza kaydı — KVKK açık rıza

Her rıza tipi için ayrı satır. Kullanıcı rızayı geri çekerse satır **silinmez**,
`revoked_at` doldurulur.

```kotlin
supabase.from("consents").insert(
  listOf(
    mapOf("kind" to "health_data",      "text_version" to "saglik-verisi-v1.0"),
    mapOf("kind" to "location",         "text_version" to "konum-v1.0"),
    mapOf("kind" to "notifications",    "text_version" to "bildirim-v1.0")
  )
)
```

`kind` değerleri: `health_data` | `location` | `emergency_contact` | `notifications`

**Rıza geri çekme:**
```kotlin
supabase.from("consents")
  .update(mapOf("revoked_at" to Instant.now().toString()))
  .eq("kind", "location")
  .isNull("revoked_at")
```

---

## 2. Sağlık verisi senkronizasyonu

### 2.1 Toplu gönderim

**Tablo:** `health_samples` — doğrudan insert.

```kotlin
supabase.from("health_samples").insert(samples)
```

Her örnek:

| Alan | Tip | Zorunlu | Not |
|---|---|---|---|
| `client_uid` | text | ✅ | Deterministik üretilir — aşağıya bak |
| `metric` | enum | ✅ | Aşağıdaki listeden |
| `value` | double | ✅ | |
| `unit` | text | | `bpm`, `ms`, `min`, `%`, `count` |
| `start_time` | timestamptz | ✅ | |
| `end_time` | timestamptz | | Aralıklı veriler için |
| `source` | text | | Varsayılan `health_connect` |

**`metric` değerleri:**
`heart_rate` · `resting_heart_rate` · `hrv` · `sleep_session` · `sleep_stage` ·
`stress` · `spo2` · `steps` · `screen_time` · `app_open`

### 2.2 `client_uid` üretimi — KRİTİK

Rastgele UUID **kullanma**. Aynı veri Health Connect'ten tekrar okunduğunda
çift kayıt oluşur. Deterministik üret:

```kotlin
fun clientUid(metric: String, startTime: Instant, source: String): String {
    val raw = "$metric|${startTime.toEpochMilli()}|$source"
    return MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(32)
}
```

Veritabanında `unique (user_id, client_uid)` var. Aynı kayıt ikinci kez
gelirse insert hata döner — bu **beklenen** durumdur, sessizce yut:

```kotlin
supabase.from("health_samples")
  .upsert(samples, onConflict = "user_id,client_uid", ignoreDuplicates = true)
```

### 2.3 Offline kuyruk

Telefon çevrimdışıyken veriyi yerel Room veritabanına yaz, bağlantı gelince
gönder. `client_uid` deterministik olduğu için tekrar gönderim güvenlidir.

**Parti büyüklüğü:** tek istekte en fazla 500 örnek.

### 2.4 Nereden devam edileceğini bulma

En son gönderilen kaydın zamanını sor, Health Connect'ten oradan itibaren oku:

```kotlin
supabase.from("health_samples")
  .select("start_time")
  .eq("metric", "heart_rate")
  .order("start_time", Order.DESCENDING)
  .limit(1)
```

---

## 3. Günlük özet ve risk skoru — sadece okuma

Bu tablolara istemci **yazamaz** (RLS `select` ile sınırlı).

### 3.1 Günlük özet

```kotlin
supabase.from("daily_aggregates")
  .select()
  .gte("day", "2026-07-25")
  .order("day", Order.DESCENDING)
```

Dönen alanlar: `day`, `sleep_minutes`, `sleep_efficiency`, `avg_hrv`,
`resting_hr`, `avg_stress`, `step_count`, `screen_time_min`, `night_screen_min`

### 3.2 Risk skoru

```kotlin
supabase.from("risk_scores")
  .select()
  .order("computed_at", Order.DESCENDING)
  .limit(1)
```

```json
{
  "id": "…",
  "for_day": "2026-08-01",
  "score": 68,
  "level": "moderate",
  "factors": {
    "sleep_drop":   { "z": -1.8, "weight": 0.3, "label": "Uyku süresi düştü" },
    "hrv_drop":     { "z": -2.1, "weight": 0.3, "label": "HRV normalin altında" },
    "night_screen": { "z":  1.4, "weight": 0.2, "label": "Gece ekran süresi arttı" }
  },
  "model_version": "rule-v1"
}
```

**UI ekibine not:** `factors` içindeki `label` alanlarını kullanıcıya doğrudan
göster. "Skorun 68" demek yetmez; **neden** olduğunu göstermek hem etik
zorunluluk hem de jüri sunumunda savunulabilirliğin temeli.

`level` değerleri: `low` (0-39) · `moderate` (40-69) · `high` (70-100)

---

## 4. Riskli lokasyonlar

### 4.1 Zone yönetimi — doğrudan tablo

Zone'ları **yalnızca kullanıcı** ekler. Uygulama arka planda gizli lokasyon
tanımlamaz. Bu bir mahremiyet taahhüdüdür, raporda da böyle yazılacak.

```kotlin
// Ekleme
supabase.from("geofence_zones").insert(
  mapOf(
    "label" to "Eski mahalle",
    "latitude" to 37.8746,
    "longitude" to 32.4932,
    "radius_m" to 200
  )
)

// Listeleme
supabase.from("geofence_zones").select().eq("is_active", true)

// Pasife alma (silmek yerine)
supabase.from("geofence_zones")
  .update(mapOf("is_active" to false)).eq("id", zoneId)
```

`radius_m`: 50–2000 arası. Varsayılan 150.

### 4.2 Geofence olayı bildirme

```kotlin
supabase.from("geofence_events").insert(
  mapOf(
    "zone_id" to zoneId,
    "event_type" to "enter",     // enter | exit | dwell
    "dwell_sec" to null,
    "occurred_at" to Instant.now().toString()
  )
)
```

**Ham koordinat gönderilmez.** Sadece hangi zone'a girildiği kaydedilir.
Kullanıcının gün içi hareket izi hiçbir yerde birikmez.

**Android tarafı:** `GeofencingClient` kullanın, ham GPS polling yapmayın —
pil tüketimi projeyi bitirir.

---

## 5. Ödül sistemi — sadece okuma

Puanı **istemci yazamaz**. Puan veren tek yer sunucudaki Edge Function'dır.

```kotlin
// Bakiye
supabase.from("v_points_balance").select().single()   // { balance: 340 }

// Hareket geçmişi
supabase.from("points_ledger")
  .select().order("created_at", Order.DESCENDING).limit(50)

// Rozet kataloğu (herkese açık)
supabase.from("badges").select()

// Kazanılan rozetler
supabase.from("user_badges").select("badge_code, earned_at")
```

`reason` değerleri: `daily_checkin` · `clean_day` · `zone_avoided` ·
`sleep_goal` · `streak_bonus`

---

## 6. Acil kişiler

```kotlin
supabase.from("emergency_contacts").insert(
  mapOf(
    "name" to "Mehmet",
    "phone" to "+905xxxxxxxxx",
    "relation" to "Kardeş",
    "priority" to 1
  )
)
```

`verified_at` alanı **sunucu tarafından** doldurulur. Doğrulanmamış kişiye
bildirim gönderilmez — yanlışlıkla girilen bir numaraya kriz bildirimi
gitmesini engeller.

---

## 7. Edge Function'lar

Çağrım şekli:

```kotlin
val result = supabase.functions.invoke("fonksiyon-adi") {
    setBody(mapOf("key" to "value"))
}
```

### 7.1 `POST /functions/v1/compute-risk`

Günlük özeti hesaplar ve risk skoru üretir. Normalde `pg_cron` ile
otomatik çalışır; istemci sadece "yenile" butonu için çağırabilir.

İstek: `{ "day": "2026-08-01" }` (opsiyonel, varsayılan bugün)

Yanıt:
```json
{ "score": 68, "level": "moderate", "risk_score_id": "…", "escalation_started": true }
```

`escalation_started` true dönerse kullanıcıya birazdan check-in bildirimi
gidecek demektir.

### 7.2 `POST /functions/v1/checkin-respond`

Kullanıcı check-in bildirimine cevap verdiğinde çağrılır.

İstek:
```json
{ "escalation_id": "…", "response": "ok" }
```

`response`: `ok` | `struggling`

Yanıt (`ok`):
```json
{ "state": "resolved", "points_awarded": 10 }
```

Yanıt (`struggling`):
```json
{
  "state": "support_offered",
  "resources": [
    { "type": "hotline", "label": "Yeşilay Danışmanlık — ALO 191", "value": "191" },
    { "type": "contact", "label": "Acil kişini ara", "value": "+905xxxxxxxxx" }
  ]
}
```

**UI ekibine not:** `struggling` ekranı sakin ve sade olmalı. Kırmızı renk,
alarm ikonu, panik yaratan dil yok. Kullanıcı zaten zorlanıyor.

### 7.3 `POST /functions/v1/register-device`

FCM token kaydı. Uygulama her açılışta ve token yenilendiğinde çağırır.

İstek: `{ "fcm_token": "…", "platform": "android" }`
Yanıt: `{ "ok": true }`

### 7.4 `POST /functions/v1/verify-contact`

Acil kişiye doğrulama SMS'i / bildirimi gönderir.

İstek: `{ "contact_id": "…" }`
Yanıt: `{ "ok": true, "sent_at": "…" }`

### 7.5 `POST /functions/v1/demo/simulate-risk`

**Sadece demo günü için.** Sahnede canlı veri akışına güvenmemek adına
yapay yüksek risk skoru üretir ve eskalasyon akışını tetikler.

İstek: `{ "level": "high" }`

Üretim ortamında feature flag ile kapatılacak.

---

## 8. Push bildirimleri (FCM)

Sunucudan gelen bildirim tipleri — mobil tarafın `data` payload'ına göre
yönlendirme yapması gerekir:

| `type` | Anlamı | Mobil davranış |
|---|---|---|
| `checkin` | Risk tespit edildi, "iyi misin?" | Check-in ekranını aç, `escalation_id` taşı |
| `zone_enter` | Riskli bölgeye girildi | Destekleyici mesaj göster |
| `badge_earned` | Rozet kazanıldı | Kutlama ekranı |
| `daily_summary` | Günlük özet hazır | Ana ekranı yenile |

Örnek payload:
```json
{
  "type": "checkin",
  "escalation_id": "…",
  "title": "Nasılsın?",
  "body": "Son birkaç gündür uykun düzensiz görünüyor. Bir dakikan var mı?"
}
```

---

## 9. Veri silme hakkı (KVKK md. 7)

```kotlin
supabase.functions.invoke("delete-my-data")
```

Tüm kullanıcı verisini siler (cascade), `auth.users` kaydını kaldırır,
`audit_log`'a anonim bir silme kaydı bırakır.

**Ayarlar ekranında bu düğme mutlaka bulunmalı.** Jüri raporda bunu arayacak.

---

## 10. Sürüm notları

| Sürüm | Tarih | Değişiklik |
|---|---|---|
| v1 | 2026-08-01 | İlk sözleşme |
