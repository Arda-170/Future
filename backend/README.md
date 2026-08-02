# TEKNOFEST — Bağımlılıklarla Mücadelede Teknolojik Uygulamalar
## Backend

Akıllı saat verisinden nüks riskini erken tespit eden ve kademeli destek
mekanizması işleten sistemin sunucu tarafı.

---

## Ne yapıyor

1. **Veri toplama** — Galaxy Watch → Samsung Health → Health Connect → mobil uygulama → bu backend
2. **Risk hesaplama** — kişiye özel temel çizgiye göre sapma analizi (z-skoru)
3. **Müdahale zinciri** — risk tespit edilirse kademeli destek: kontrol mesajı → destek kaynakları → acil kişi
4. **Ödül sistemi** — davranış değişimini destekleyen puan ve rozet mekanizması

---

## Mimari

| Katman | Teknoloji | Neden |
|---|---|---|
| Veritabanı | Supabase (PostgreSQL) | Zaman serisi veri için uygun, RLS ile veritabanı seviyesinde güvenlik |
| Sunucu mantığı | Supabase Edge Functions (Deno/TypeScript) | Kullanıcı lehine sonuç doğuran işlemler istemcide çalışmamalı |
| Zamanlayıcı | pg_cron | Eskalasyon zaman aşımı ve gecelik hesaplamalar |
| Push bildirim | Firebase Cloud Messaging (HTTP v1) | Android'de zorunlu; başka yolu yok |

**Temel güvenlik ilkesi:** İstemci girdisine güvenilmez. Kullanıcı kimliği
her zaman JWT'den türetilir; risk skoru, puan ve rozet yalnızca sunucu yazar.

---

## Klasör yapısı

```
teknofest-backend/
├── db/
│   ├── schema.sql              # Tablolar, RLS politikaları, indeksler
│   ├── risk-engine.sql         # Risk hesaplama fonksiyonları
│   ├── escalation-scheduler.sql# Eskalasyon zamanlayıcısı
│   └── badges-geofence.sql     # Rozet otomasyonu, geofence tetikleyici
├── supabase/
│   └── functions/
│       ├── _shared/fcm.ts      # FCM HTTP v1 istemcisi (ortak)
│       ├── register-device/    # FCM token kaydı
│       ├── compute-risk/       # Risk hesaplama + eskalasyon tetikleme
│       ├── checkin-respond/    # Kontrol mesajına kullanıcı yanıtı
│       ├── verify-contact/     # Acil kişi doğrulama
│       ├── delete-my-data/     # KVKK md.7 — veri silme hakkı
│       ├── process-queue/      # Bildirim kuyruğu drenajı (cron)
│       └── simulate-risk/      # Demo endpoint (üretimde kapalı)
├── api-sozlesmesi.md           # Mobil ve UI ekibi için API dokümantasyonu
└── .env.example                # Gerekli ortam değişkenleri (değersiz)
```

---

## Mobil ekip için

Panele erişmenize gerek yok. İhtiyacınız olan iki değer:

```
SUPABASE_URL      = https://<proje-ref>.supabase.co
SUPABASE_ANON_KEY = sb_publishable_...
```

`anon` anahtarı uygulamaya gömülmek üzere tasarlanmıştır, gizli değildir —
veri güvenliğini RLS sağlar.

Hangi çağrının nasıl yapılacağı **`api-sozlesmesi.md`** dosyasında.
Özellikle şu iki bölüm kritik:

- **Bölüm 2.2** — `client_uid` deterministik üretilmeli, rastgele UUID değil
- **Bölüm 3.2** — risk skorunun `factors` alanı kullanıcıya gösterilmeli

Push bildirimi için `google-services.json` dosyasını backend sorumlusundan alın.

---

## Backend geliştirme kurulumu

Yalnızca sunucu tarafında çalışacaklar için.

```bash
# 1. Supabase CLI
scoop bucket add supabase https://github.com/supabase/scoop-bucket.git
scoop install supabase

# 2. Projeye bağlan
supabase login
supabase link --project-ref <proje-ref>

# 3. Ortam değişkenleri
cp .env.example .env
# .env içini Firebase servis hesabı JSON'undaki değerlerle doldur
supabase secrets set --env-file .env

# 4. Fonksiyonları deploy et
supabase functions deploy compute-risk
supabase functions deploy process-queue --no-verify-jwt
# ...
```

Veritabanı şeması için `db/` altındaki SQL dosyalarını sırayla
Supabase SQL Editor'de çalıştırın:
`schema.sql` → `risk-engine.sql` → `escalation-scheduler.sql` → `badges-geofence.sql`

---

## Risk hesaplama yaklaşımı

Mutlak eşik kullanılmaz. Herkesin normal HRV'si, uyku süresi ve dinlenme
nabzı farklıdır; "HRV 40'ın altındaysa riskli" demek bilimsel olarak
savunulamaz.

Bunun yerine her kullanıcı için **14 günlük kişisel temel çizgi**
hesaplanır ve günlük değerin bu çizgiden kaç standart sapma uzaklaştığına
bakılır.

| Sinyal | Yön | Ağırlık |
|---|---|---|
| Uyku süresi | Düşüş riskli | 0.30 |
| HRV | Düşüş riskli | 0.25 |
| Dinlenme nabzı | Artış riskli | 0.20 |
| Gece ekran süresi | Artış riskli | 0.25 |

Eksik sinyal varsa ağırlıklar kalanlar arasında yeniden dağıtılır — veri
gelmemesi yapay olarak düşük risk üretmez. En az iki sinyal ve en az 7
günlük geçmiş olmadan skor üretilmez.

**Açıklanabilirlik:** Her skor, hangi sinyalin ne kadar katkı verdiğini
`factors` alanında saklar. Bağımlılık gibi hassas bir alanda kararını
açıklayamayan bir sistem kullanılamaz.

---

## Eskalasyon protokolü

```
Yüksek risk tespit edildi
        │
        ▼
  Kontrol mesajı ("Nasılsın?")
        │
   ┌────┴────┬──────────────┐
   │         │              │
"İyiyim"  "Zorlanıyorum"  15 dk yanıt yok
   │         │              │
   ▼         ▼              ▼
Kapatılır  Destek       Acil kişiye
+10 puan   kaynakları   bildirim
           +15 puan
```

**Otomatik 112 araması yapılmaz.** Yanlış pozitif bir acil çağrı hem
kullanıcıyı hem sistemin güvenilirliğini zedeler. Yönlendirme YEDAM
Danışma Hattı (115) ve kullanıcının önceden tanımladığı acil kişi
üzerinden yapılır.

**Soğuma kuralı:** Aynı kullanıcıya 24 saatte en fazla bir kontrol mesajı.

---

## Mahremiyet ve KVKK

Sağlık verisi KVKK kapsamında **özel nitelikli kişisel veridir**.
Sistemde alınan önlemler:

- **Veritabanı seviyesinde izolasyon** — Row Level Security; uygulama
  katmanında hata yapılsa bile kullanıcılar birbirinin verisini göremez
  (çapraz kullanıcı erişim testiyle doğrulanmıştır)
- **Versiyonlu açık rıza** — rıza metinleri sürümlenir, geri çekme kaydı
  silinmez
- **Konum verisi saklanmaz** — riskli bölge kayıtlarında yalnızca
  "hangi bölgeye girildi" bilgisi tutulur; kullanıcının gün içi hareket
  izi hiçbir yerde birikmez
- **Bölgeleri kullanıcı tanımlar** — uygulama arka planda gizli konum
  tanımlaması yapmaz
- **Acil kişiye giden mesajda tanı bilgisi yoktur** — acil kişi
  kullanıcının durumunu bilmiyor olabilir
- **Veri silme hakkı** — `delete-my-data` fonksiyonu tüm veriyi ve hesabı
  siler; denetim kaydında silinen kullanıcıya işaret eden bağ bırakılmaz
- **Denetim kaydı** — hangi işlemin ne zaman yapıldığı `audit_log`
  tablosunda tutulur

---

## Bilinen sınırlamalar

Dürüstlük adına kapsamı net tutuyoruz:

- **SMS gönderimi** sağlayıcı sözleşmesi gerektirdiğinden pilot fazına
  bırakılmıştır. Kuyruk yapısı kuruludur, gönderim simüle edilir.
- **Samsung stres skoru** kullanılmamaktadır — kapalı kutu bir algoritma
  olduğu ve Health Connect'e her zaman akmadığı için. HRV ve nabız aynı
  fizyolojiyi daha şeffaf ölçer.
- **Risk modeli kural tabanlıdır.** Makine öğrenmesi, yeterli gerçek veri
  toplandıktan sonraki fazda değerlendirilecektir. Az veriyle eğitilmiş
  bir model, açıklanabilir kural tabanlı sistemden daha güvenilir değildir.

---

## Güvenlik notu

Şu dosyalar **asla** depoya eklenmez:

- `.env`
- Firebase servis hesabı JSON'u (`*firebase-adminsdk*.json`)
- Supabase `service_role` / secret anahtarı

Bunlar sunucu tarafında ortam değişkeni olarak yaşar. Anahtar kazara
paylaşılırsa Firebase Console'dan yenisi üretilip eskisi silinmelidir.
