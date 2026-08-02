# API Sözleşmesi — v1.1

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
| **Edge Function** | Sonucu sunucunun belirlemesi gereken işler | `supabase.functions.invoke(...)` |

**Neden bu ayrım:** Kullanıcının lehine sonuç doğuran hiçbir şey istemciden
yazılamaz. Risk skoru, puan, rozet, eskalasyon → yalnızca sunucu yazar.

**Kimlik doğrulama:** Tüm çağrılarda Supabase Auth JWT'si otomatik gider.
`auth.uid()` RLS tarafından okunur.

**`user_id` göndermeyin.** İlgili tablolarda bu alanın varsayılan değeri
`auth.uid()` olarak tanımlıdır; veritabanı JWT'den otomatik doldurur.
Gönderirseniz de RLS başkasının id'sini reddeder.

**Zaman formatı:** ISO 8601 + UTC offset.
Örnek: `2026-08-01T23:14:00+03:00`

**Hata formatı (Edge Function'lar):**
```json
{ "error": { "code": "CONSENT_REQUIRED", "message": "Sağlık verisi rızası bulunamadı" } }
```

---

## 1. Onboarding akışı

Sıra önemlidir. Rıza alınmadan risk hesaplaması sunucuda reddedilir.

```
1. Kayıt / giriş        → supabase.auth.signUp / signInWithPassword
2. Profil oluştur       → profiles insert
3. Rıza ekranları       → consents insert  (her tip için ayrı satır)
4. Health Connect izni  → (Android tarafı, sunucuyu ilgilendirmez)
5. FCM token kaydı      → register-device
6. İlk senkronizasyon   → health_samples insert
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

`profiles` tablosunda birincil anahtar `id`'dir ve açıkça gönderilir —
tek istisna budur.

### 1.2 Rıza kaydı — KVKK açık rıza

Her rıza tipi için ayrı satır. Kullanıcı rızayı geri çekerse satır
**silinmez**, `revoked_at` doldurulur.

```kotlin
supabase.from("consents").insert(
  listOf(
    mapOf("kind" to "health_data",   "text_version" to "saglik-verisi-v1.0"),
    mapOf("kind" to "location",      "text_version" to "konum-v1.0"),
    mapOf("kind" to "notifications", "text_version" to "bildirim-v1.0")
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

> **Not:** Risk motoru şu dört sinyali kullanır: `sleep_session`, `hrv`,
> `resting_heart_rate`, `screen_time`. Diğerleri saklanır ama skora
> girmez. `stress` bilinçli olarak kullanılmamaktadır (bkz. README).

### 2.2 `client_uid` üretimi — KRİTİK

Rastgele UUID **kullanmayın**. Aynı veri Health Connect'ten tekrar
okunduğunda çift kayıt oluşur. Deterministik üretin:

```kotlin
fun clientUid(metric: String, startTime: Instant, source: String): String {
    val raw = "$metric|${startTime.toEpochMilli()}|$source"
    return MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(32)
}
```

Veritabanında `unique (user_id, client_uid)` kısıtı var. Aynı kayıt ikinci
kez gelirse insert hata döner — bu **beklenen** durumdur:

```kotlin
supabase.from("health_samples")
  .upsert(samples, onConflict = "user_id,client_uid", ignoreDuplicates = true)
```

### 2.3 Gece ekran süresi

`screen_time` örneklerinin `start_time` değeri doğru olmalı — sunucu
yerel saat 00:00–06:00 arasındaki kayıtları ayrıca toplar ve bunu bir risk
sinyali olarak kullanır. Günlük toplamı tek bir kayıt olarak göndermek bu
sinyali yok eder; en azından gece/gündüz ayrımı yapılmalıdır.

### 2.4 Offline kuyruk

Telefon çevrimdışıyken veriyi yerel Room veritabanına yazın, bağlantı
gelince gönderin. `client_uid` deterministik olduğu için tekrar gönderim
güvenlidir.

**Parti büyüklüğü:** tek istekte en fazla 500 örnek.

### 2.5 Nereden devam edileceğini bulma

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

Alanlar: `day`, `sleep_minutes`, `sleep_efficiency`, `avg_hrv`,
`resting_hr`, `avg_stress`, `step_count`, `screen_time_min`,
`night_screen_min`

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
  "score": 49,
  "level": "moderate",
  "factors": {
    "sleep_drop": {
      "z": 1.49, "weight": 0.30,
      "value": 410, "baseline": 428.9,
      "label": "Uyku süresi normalinin altında"
    },
    "hrv_drop": {
      "z": 1.54, "weight": 0.25,
      "value": 52.3, "baseline": 55.4,
      "label": "Kalp atım değişkenliği düşmüş"
    }
  },
  "model_version": "rule-v1"
}
```

**UI ekibine not:** `factors` içindeki `label`, `value` ve `baseline`
alanlarını kullanıcıya gösterin. `z` ve `weight` iç hesaplama detayıdır,
gösterilmez.

Örnek gösterim: *"Uyku süresi normalinin altında — 410 dk (normalin 429 dk)"*

Sadece skoru göstermek yeterli değildir. Sistem kararını açıklamalıdır.

`level` değerleri: `low` (0–39) · `moderate` (40–69) · `high` (70–100)

**Yetersiz veri durumu:** İlk 7 gün skor üretilmez. Bu durumda
`compute-risk` şunu döner:

```json
{ "status": "insufficient_data", "days_available": 3, "days_required": 7 }
```

Ekranda "veri toplanıyor" durumu gösterin, sıfır skor göstermeyin.

---

## 4. Riskli lokasyonlar

### 4.1 Zone yönetimi — doğrudan tablo

Zone'ları **yalnızca kullanıcı** ekler. Uygulama arka planda gizli lokasyon
tanımlamaz. Bu bir mahremiyet taahhüdüdür.

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

**Sunucu davranışı:** Giriş olayı eskalasyon başlatmaz. Destekleyici bir
push bildirimi gönderilir, aynı zone için 6 saatte bir.

---

## 5. Ödül sistemi — sadece okuma

Puanı **istemci yazamaz**. Puan veren tek yer sunucudur.

```kotlin
// Bakiye
supabase.from("v_points_balance").select().single()   // { balance: 25 }

// Hareket geçmişi
supabase.from("points_ledger")
  .select().order("created_at", Order.DESCENDING).limit(50)

// Rozet kataloğu (herkese açık)
supabase.from("badges").select()

// Kazanılan rozetler
supabase.from("user_badges").select("badge_code, earned_at")

// Kesintisiz seri (gün sayısı)
supabase.rpc("get_checkin_streak", mapOf("p_user" to userId))
```

`reason` değerleri: `daily_checkin` (10) · `reached_out` (15) ·
`clean_day` (5) · `sleep_goal` · `streak_bonus`

`badge_code` değerleri: `first_step` · `week_streak` · `month_streak` ·
`good_sleep` · `zone_avoided` · `reached_out`

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

`verified_at` alanı **sunucu tarafından** doldurulur ve doğrulama akışı
zorunludur (bkz. 7.4). Doğrulanmamış kişiye bildirim gönderilmez.

---

## 7. Edge Function'lar

```kotlin
val result = supabase.functions.invoke("fonksiyon-adi") {
    setBody(mapOf("key" to "value"))
}
```

### 7.1 `compute-risk`

Günlük özeti üretir ve risk skoru hesaplar. Normalde her gece 03:00'te
otomatik çalışır; istemci "yenile" butonu için de çağırabilir.

İstek: `{ "day": "2026-08-01" }` (opsiyonel, varsayılan bugün)

Yanıt:
```json
{
  "status": "ok",
  "score": 49,
  "level": "moderate",
  "factors": { },
  "baseline_days": 14,
  "escalation_started": false
}
```

`escalation_started` true dönerse kullanıcıya birazdan check-in bildirimi
gidecek demektir.

Rıza yoksa `403 CONSENT_REQUIRED` döner.

### 7.2 `checkin-respond`

Kullanıcı check-in bildirimine cevap verdiğinde çağrılır.

İstek:
```json
{ "escalation_id": "…", "response": "ok" }
```

`response`: `ok` | `struggling`

Yanıt (`ok`):
```json
{ "state": "resolved", "points_awarded": 10, "message": "İyi olduğunu duymak güzel." }
```

Yanıt (`struggling`):
```json
{
  "state": "support_offered",
  "resources": [
    { "type": "hotline", "label": "YEDAM Danışma Hattı — 115",
      "value": "115", "note": "Ücretsiz ve gizli" },
    { "type": "contact", "label": "Mehmet — acil kişin", "value": "+905xxxxxxxxx" }
  ],
  "message": "Bunu söylemek cesaret ister. Yalnız değilsin."
}
```

`resources` dizisi doğrulanmış acil kişi yoksa yalnızca hattı içerir.

**UI ekibine not:** `struggling` ekranı sakin ve sade olmalı. Kırmızı renk,
alarm ikonu, panik yaratan dil yok. Kullanıcı zaten zorlanıyor.

Kapanmış bir eskalasyona tekrar cevap verilirse `409 ALREADY_CLOSED` döner.

### 7.3 `register-device`

FCM token kaydı. Uygulama her açılışta ve token yenilendiğinde çağırır.

İstek: `{ "fcm_token": "…", "platform": "android" }`
Yanıt: `{ "ok": true }`

### 7.4 `verify-contact`

İki aşamalı akış.

**Aşama 1 — kod iste:**
```json
{ "contact_id": "…" }
```
```json
{ "status": "code_sent", "expires_at": "…" }
```

**Aşama 2 — kodu doğrula:**
```json
{ "contact_id": "…", "code": "483920" }
```
```json
{ "status": "verified", "contact_name": "Mehmet" }
```

Kod 15 dakika geçerlidir, 5 hatalı denemeden sonra kilitlenir. Yeni kod
istenirse eskisi geçersiz olur.

Doğrulama SMS'inde kullanıcının durumu hakkında hiçbir bilgi yoktur.

### 7.5 `delete-my-data` — KVKK md. 7

```json
{ "confirm": "SIL" }
```

Onay alanı olmadan `428 CONFIRMATION_REQUIRED` döner.

Yanıt:
```json
{
  "status": "deleted",
  "records_removed": { "health_samples": 412, "risk_scores": 20 },
  "message": "Tüm verilerin silindi. Bu işlem geri alınamaz."
}
```

**Ayarlar ekranında bu düğme mutlaka bulunmalı.** Çift onay ekranı
kullanın; işlem geri alınamaz.

### 7.6 `simulate-risk` — DEMO

Sadece demo günü için. Yapay risk skoru üretip eskalasyon zincirini
tetikler. `DEMO_MODE` kapalıysa `403` döner.

İstek: `{ "level": "high", "bypass_cooldown": true }`

Ürettiği skorlar veritabanında `model_version: "demo-sim"` olarak
işaretlenir, gerçek skorlardan ayırt edilebilir.

---

## 8. Push bildirimleri (FCM)

Bildirimler **data-only** gönderilir — görünümü ve davranışı mobil taraf
kontrol eder. Kriz anı arayüzü için bu önemli.

| `type` | Anlamı | Mobil davranış |
|---|---|---|
| `checkin` | Risk tespit edildi | Check-in ekranını aç, `escalation_id` taşı |
| `zone_enter` | Riskli bölgeye girildi | Destekleyici mesaj göster |
| `badge_earned` | Rozet kazanıldı | Kutlama ekranı |
| `daily_summary` | Günlük özet hazır | Ana ekranı yenile |

Örnek payload:
```json
{
  "type": "checkin",
  "escalation_id": "…",
  "title": "Nasılsın?",
  "body": "Son birkaç gündür verilerinde değişiklik var. Bir dakikan var mı?"
}
```

FCM `data` alanı yalnızca string kabul eder; sayısal değerler string
olarak gelir.

---

## 9. Sürüm notları

| Sürüm | Tarih | Değişiklik |
|---|---|---|
| v1.1 | 2026-08-02 | YEDAM hattı 115 olarak düzeltildi (191 yanlıştı); `user_id` varsayılanı eklendi, istemci artık göndermiyor; `process-queue`, `get_checkin_streak` ve rozet kodları eklendi; yetersiz veri yanıtı belgelendi |
| v1 | 2026-08-01 | İlk sözleşme |
