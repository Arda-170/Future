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
| Zamanlayıcı | pg_cron + pg_net | Eskalasyon zaman aşımı, gecelik hesaplamalar, kuyruk drenajı |
| Push bildirim | Firebase Cloud Messaging (HTTP v1) | Android'de zorunlu; başka yolu yok |

**Temel güvenlik ilkesi:** İstemci girdisine güvenilmez. Kullanıcı kimliği
her zaman JWT'den türetilir; risk skoru, puan ve rozet yalnızca sunucu yazar.

**Not:** Firebase'den yalnızca Cloud Messaging kullanılmaktadır. Veritabanı,
kimlik doğrulama ve sunucu mantığı tamamen Supabase üzerindedir.

---

## Depo yapısı

```
Future/
├── .gitignore
├── wearable/                        # Mobil uygulama (Android)
└── backend/                         # Sunucu tarafı — bu doküman
    ├── README.md
    ├── api-sozlesmesi.md            # Mobil ve UI ekibi için API dokümantasyonu
    ├── .env.example                 # Gerekli ortam değişkenleri (değersiz şablon)
    ├── make-env.ps1                 # Firebase JSON'undan .env üreten yardımcı
    ├── db/
    │   ├── schema.sql               # Tablolar, RLS politikaları, indeksler
    │   ├── risk-engine.sql          # Risk hesaplama fonksiyonları
    │   ├── escalation-scheduler.sql # Eskalasyon zamanlayıcısı, bildirim kuyruğu
    │   ├── badges-geofence.sql      # Rozet otomasyonu, geofence tetikleyici
    │   └── seed-test-data.sql       # Sentetik test verisi üreteci
    └── supabase/
        ├── config.toml
        └── functions/
            ├── _shared/fcm.ts       # FCM HTTP v1 istemcisi (ortak modül)
            ├── register-device/     # FCM token kaydı
            ├── compute-risk/        # Risk hesaplama + eskalasyon tetikleme
            ├── checkin-respond/     # Kontrol mesajına kullanıcı yanıtı
            ├── verify-contact/      # Acil kişi doğrulama
            ├── delete-my-data/      # KVKK md.7 — veri silme hakkı
            ├── process-queue/       # Bildirim kuyruğu drenajı (cron çağırır)
            └── simulate-risk/       # Demo endpoint (üretimde kapalı)
```

---

## Mobil ekip için

Supabase paneline erişmenize gerek yok. İhtiyacınız olan iki değer:

```
SUPABASE_URL      = https://<proje-ref>.supabase.co
SUPABASE_ANON_KEY = sb_publishable_...
```

`anon` (publishable) anahtarı uygulamaya gömülmek üzere tasarlanmıştır,
gizli değildir — veri güvenliğini RLS sağlar.

Hangi çağrının nasıl yapılacağı **`api-sozlesmesi.md`** dosyasında.
Özellikle şu iki bölüm kritik:

- **Bölüm 2.2** — `client_uid` deterministik üretilmeli, rastgele UUID
  **değil**. Aksi halde her senkronizasyonda veri katlanır.
- **Bölüm 3.2** — risk skorunun `factors` alanı kullanıcıya gösterilmeli.
  Sadece skoru göstermek yeterli değil; sistem kararını açıklamalı.

Push bildirimi için `google-services.json` dosyasını backend sorumlusundan
alın.

---

## Backend geliştirme kurulumu

Yalnızca sunucu tarafında çalışacaklar için.

```bash
# 1. Supabase CLI (Windows / scoop)
scoop bucket add supabase https://github.com/supabase/scoop-bucket.git
scoop install supabase

# 2. Projeye bağlan
cd backend
supabase login
supabase link --project-ref <proje-ref>

# 3. Ortam değişkenleri
#    Firebase servis hesabı JSON'unu indir, sonra:
.\make-env.ps1 -JsonPath "<indirilen-json-yolu>"
supabase secrets set --env-file .env
supabase secrets set DEMO_MODE=true
supabase secrets set QUEUE_SECRET=<rastgele-32-karakter>

# 4. Fonksiyonları deploy et
supabase functions deploy register-device
supabase functions deploy compute-risk
supabase functions deploy checkin-respond
supabase functions deploy verify-contact
supabase functions deploy delete-my-data
supabase functions deploy simulate-risk
supabase functions deploy process-queue --no-verify-jwt
```

`process-queue` fonksiyonu `--no-verify-jwt` ile deploy edilir, çünkü onu
kullanıcı değil zamanlayıcı çağırır; JWT yerine `QUEUE_SECRET` başlığıyla
korunur.

**Veritabanı kurulumu** — `db/` altındaki SQL dosyalarını bu sırayla
Supabase SQL Editor'de çalıştırın:

```
schema.sql → risk-engine.sql → escalation-scheduler.sql → badges-geofence.sql
```

Test verisi isterseniz en son `seed-test-data.sql` (içindeki kullanıcı
id'sini kendi test kullanıcınızla değiştirin).

---

## Risk hesaplama yaklaşımı

Mutlak eşik kullanılmaz. Herkesin normal HRV'si, uyku süresi ve dinlenme
nabzı farklıdır; "HRV 40'ın altındaysa riskli" demek bilimsel olarak
savunulamaz.

Bunun yerine her kullanıcı için **14 günlük kişisel temel çizgi**
(ortalama ve standart sapma) hesaplanır ve günlük değerin bu çizgiden kaç
standart sapma uzaklaştığına bakılır.

| Sinyal | Riskli yön | Ağırlık |
|---|---|---|
| Uyku süresi | Düşüş | 0.30 |
| HRV (kalp atım değişkenliği) | Düşüş | 0.25 |
| Dinlenme nabzı | Artış | 0.20 |
| Gece ekran süresi (00:00–06:00) | Artış | 0.25 |

**Eksik veri yönetimi:** Bir sinyal gelmezse ağırlığı kalanlar arasında
yeniden dağıtılır. Bu olmadan, veri gelmemesi yapay olarak düşük risk
üretirdi — sessiz ve tehlikeli bir hata türü.

**Alt sınırlar:** En az 7 günlük geçmiş ve en az iki sinyal olmadan skor
üretilmez. Bu durumda sistem "yeterli veri toplanmadı" der; uydurma skor
göstermez.

**Üst sınır:** z değeri 3'te kırpılır. Tek bir bozuk ölçüm skoru uçuramaz.

**Açıklanabilirlik:** Her skor, hangi sinyalin ne kadar katkı verdiğini
`factors` alanında saklar:

```json
{
  "sleep_drop": {
    "z": 1.49, "weight": 0.30,
    "value": 410, "baseline": 428.9,
    "label": "Uyku süresi normalinin altında"
  }
}
```

Bağımlılık gibi hassas bir alanda kararını açıklayamayan bir sistem
kullanılamaz.

**Samsung stres skoru kullanılmamaktadır** — kapalı kutu bir algoritma
olduğu ve Health Connect'e her zaman tam granülerlikte akmadığı için.
HRV ve dinlenme nabzı aynı fizyolojiyi daha şeffaf ölçer.

---

## Eskalasyon protokolü

```
Yüksek risk tespit edildi (skor ≥ 70)
        │
        ▼
  Kontrol mesajı ("Nasılsın?")
        │
   ┌────┴─────────┬──────────────────┐
   │              │                  │
"İyiyim"    "Zorlanıyorum"     15 dk yanıt yok
   │              │                  │
   ▼              ▼                  ▼
Kapatılır    Destek kaynakları   Acil kişiye
+10 puan     +15 puan            bildirim
```

**Otomatik 112 araması yapılmaz.** Yanlış pozitif bir acil çağrı hem
kullanıcıyı hem sistemin güvenilirliğini zedeler. Yönlendirme **YEDAM
Danışma Hattı (115)** ve kullanıcının önceden tanımlayıp doğruladığı acil
kişi üzerinden yapılır.

**Soğuma kuralı:** Aynı kullanıcıya 24 saatte en fazla bir kontrol mesajı.
Yanlış pozitifler kullanıcıyı uygulamadan soğutur; bağımlılık alanında bir
kullanıcıyı kaybetmek en pahalı hatadır.

**Yardım istemek daha çok ödüllendirilir** — 15 puan, "iyiyim" cevabının
10 puanına karşılık. Contingency management yaklaşımında teşvik edilmesi
gereken davranış, sorunu saklamak değil dile getirmektir.

**Geofence eskalasyon başlatmaz.** Riskli bölgeye girmek tek başına kriz
göstergesi değildir — kişi işe giderken oradan geçiyor olabilir. Sadece
destekleyici bir mesaj gönderilir, o da aynı bölge için 6 saatte bir.

---

## Bildirim kuyruğu

Bildirim **üretimi** (veritabanı trigger'ları) ile **gönderimi** (FCM/SMS)
ayrılmıştır. Teknik sebep: veritabanı trigger'ı içinden dış servise HTTP
isteği atılamaz. Yan faydası: gönderim başarısız olursa yeniden denenir,
üç denemeden sonra durur, tüm süreç izlenebilir kalır.

`process-queue` fonksiyonu pg_cron tarafından dakikada bir çağrılır.

---

## Mahremiyet ve KVKK

Sağlık verisi KVKK kapsamında **özel nitelikli kişisel veridir**.
Alınan önlemler:

- **Veritabanı seviyesinde izolasyon** — Row Level Security. Uygulama
  katmanında hata yapılsa bile kullanıcılar birbirinin verisini göremez.
  *Çapraz kullanıcı erişim testiyle doğrulanmıştır: A kullanıcısının
  JWT'siyle 2 kayıt dönerken, B kullanıcısının JWT'siyle aynı endpoint
  boş küme döndürmüştür.*
- **Versiyonlu açık rıza** — rıza metinleri sürümlenir; geri çekme kaydı
  silinmez, `revoked_at` doldurulur.
- **Konum verisi saklanmaz** — riskli bölge kayıtlarında yalnızca "hangi
  bölgeye girildi" bilgisi tutulur. Kullanıcının gün içi hareket izi
  hiçbir yerde birikmez.
- **Bölgeleri kullanıcı tanımlar** — uygulama arka planda gizli konum
  tanımlaması yapmaz.
- **Acil kişiye giden mesajda tanı bilgisi yoktur** — acil kişi
  kullanıcının durumunu bilmiyor olabilir. Mesaj yalnızca "yanıt vermedi,
  arar mısın?" der; madde adı veya "bağımlılık" kelimesi geçmez.
- **Acil kişi doğrulaması zorunludur** — doğrulanmamış numaraya bildirim
  gönderilmez. Yanlış girilmiş bir numaraya kriz bildirimi gitmesi hem
  mahremiyet ihlali hem de üçüncü bir kişiyi gereksiz kaygıya sokmaktır.
- **Veri silme hakkı** — `delete-my-data` tüm veriyi ve hesabı siler.
  Denetim kaydında silinen kullanıcıya işaret eden bağ bırakılmaz;
  yalnızca "bir silme talebi karşılandı" bilgisi tutulur.
- **Denetim kaydı** — hangi işlemin ne zaman yapıldığı `audit_log`
  tablosunda saklanır.

---

## Test durumu

| Bileşen | Durum |
|---|---|
| Şema, RLS, çapraz kullanıcı izolasyonu | Test edildi |
| Risk motoru — düşük / orta / yüksek bantlar | Test edildi |
| Eskalasyon — kullanıcı yanıtı dalı | Test edildi |
| Eskalasyon — yanıt yok dalı (zamanlayıcı) | Test edildi |
| FCM HTTP v1 kimlik doğrulama zinciri | Test edildi |
| Bildirim kuyruğu ve drenajı | Test edildi |
| Acil kişi doğrulama | Test edildi |
| Geofence tetikleyici | Test edildi |
| Rozet otomasyonu | Kurulu |
| KVKK veri silme | Kod hazır |
| Gerçek cihaz entegrasyonu | **Bekliyor** |

---

## Bilinen sınırlamalar

Kapsamı dürüst tutuyoruz:

- **SMS gönderimi** sağlayıcı sözleşmesi (Netgsm, İletimerkezi vb.)
  gerektirdiğinden pilot fazına bırakılmıştır. Kuyruk yapısı kuruludur,
  gönderim simüle edilir ve kayıt altına alınır.
- **Risk modeli kural tabanlıdır.** Makine öğrenmesi, yeterli gerçek veri
  toplandıktan sonraki fazda değerlendirilecektir. Az veriyle eğitilmiş
  bir model, açıklanabilir kural tabanlı bir sistemden daha güvenilir
  değildir.
- **Health Connect veri kapsamı** — Samsung bazı verileri (stres skoru,
  HRV detayları) yalnızca kendi SDK'sı üzerinden tam granülerlikte verir.
  Sistem bu sinyallerin eksikliğine dayanıklı tasarlanmıştır.

---

## Güvenlik notu

Şu dosyalar **asla** depoya eklenmez:

- `.env`
- Firebase servis hesabı JSON'u (`*firebase-adminsdk*.json`)
- Supabase secret (`service_role`) anahtarı

Bunlar sunucu tarafında ortam değişkeni olarak yaşar ve `.gitignore` ile
korunur. Anahtar kazara paylaşılırsa Firebase Console'dan yenisi üretilip
eskisi silinmelidir — yeni anahtar üretmek eskisini otomatik geçersiz
kılmaz.
