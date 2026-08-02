package com.example.myapplication

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.provider.Settings
import android.os.Process
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {

    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(this)
    }

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    // Ekranda gösterilecek veriler
    private var stepCountState = mutableStateOf<Long?>(null)
    private var avgHeartRateState = mutableStateOf<Long?>(null)
    private var latestHeartRateState = mutableStateOf<Long?>(null)
    private var sleepDurationState = mutableStateOf<String?>(null)
    private var exerciseSummaryState = mutableStateOf<String?>(null)
    private var statusMessage = mutableStateOf("Henüz veri çekilmedi")
    private var screenTimeState = mutableStateOf<String?>(null)
    private var tahminiUykuState = mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(permissions)) {
            Log.d("HealthConnect", "İzinler verildi, veri okunuyor...")
            tumVerileriCek()
        } else {
            Log.d("HealthConnect", "İzinler reddedildi")
            statusMessage.value = "İzinler reddedildi"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                HomeScreen()
                //CrisisScreen()
            }
        }

        /*
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnaEkran(
                        adimSayisi = stepCountState.value,
                        ortalamaNabiz = avgHeartRateState.value,
                        sonNabiz = latestHeartRateState.value,
                        uykuSuresi = sleepDurationState.value,
                        egzersizOzeti = exerciseSummaryState.value,
                        ekranSuresi = screenTimeState.value,
                        tahminiUyku = tahminiUykuState.value,
                        durumMesaji = statusMessage.value,
                        onYenileTiklandi = { checkHealthConnectDurumu() },
                        onIzinIste = { kullanimErisimiAyarlarinaGit() }
                    )
                }
            }
        }*/
        checkHealthConnectDurumu()
    }

    private fun checkHealthConnectDurumu() {
        val status = HealthConnectClient.getSdkStatus(this)

        when (status) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                statusMessage.value = "Bu cihaz Health Connect'i desteklemiyor"
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                statusMessage.value = "Health Connect güncellemesi/kurulumu gerekiyor"
            }

            HealthConnectClient.SDK_AVAILABLE -> {
                checkAndRequestPermissions()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(permissions)) {
                tumVerileriCek()
            } else {
                requestPermissionLauncher.launch(permissions)
            }
        }
    }

    // Tüm verileri sırayla çeken ana fonksiyon
    private fun tumVerileriCek() {
        lifecycleScope.launch {
            statusMessage.value = "Veriler çekiliyor..."

            kaynaklariGoster()

            val adimSayisi = readTodaySteps()
            stepCountState.value = adimSayisi
            Log.d("HealthConnect", "Bugünkü adım sayısı: $adimSayisi")

            val (ortalama, sonDeger) = readTodayHeartRate()
            avgHeartRateState.value = ortalama
            latestHeartRateState.value = sonDeger
            Log.d("HealthConnect", "Ortalama nabız: $ortalama, Son nabız: $sonDeger")

            val uyku = readLastNightSleep()
            sleepDurationState.value = uyku
            Log.d("HealthConnect", "Uyku süresi: $uyku")

            val egzersiz = readTodayExercise()
            exerciseSummaryState.value = egzersiz
            Log.d("HealthConnect", "Egzersiz özeti: $egzersiz")

            // Ekran süresi
            val ekranDk = readScreenTimeToday()
            screenTimeState.value =
                if (ekranDk >= 0) "${ekranDk / 60} sa ${ekranDk % 60} dk" else "İzin gerekli"
            Log.d("HealthConnect", "Ekran süresi (dk): $ekranDk")

            // Eğer saatten uyku verisi gelmediyse, ekran kapalı süresinden tahmin et
            if (uyku == "Veri yok") {
                tahminiUykuState.value = estimateSleepFromScreenOff()

            } else {
                tahminiUykuState.value = null
            }

            statusMessage.value = "Son güncelleme: şimdi"
        }
    }

    // --- BUGÜNÜN BAŞLANGICI (telefonun saat dilimine göre) ---
    private fun bugununBaslangici(): Instant {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        return today.atStartOfDay(zoneId).toInstant()
    }

    // --- ADIM SAYISI ---
    private suspend fun readTodaySteps(): Long {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(bugununBaslangici(), Instant.now())
            )
        )
        return response.records.sumOf { it.count }
    }

    private suspend fun kaynaklariGoster() {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(bugununBaslangici(), Instant.now())
            )
        )
        val kaynaklar = response.records.map { it.metadata.dataOrigin.packageName }.distinct()
        Log.d("HealthConnect", "Adım verisi kaynakları: $kaynaklar")
    }

    // --- KALP ATIŞI (ortalama ve son değer) ---
    private suspend fun readTodayHeartRate(): Pair<Long?, Long?> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(bugununBaslangici(), Instant.now())
            )
        )

        val tumOlcumler = response.records.flatMap { it.samples }
        if (tumOlcumler.isEmpty()) return Pair(null, null)

        val ortalama = tumOlcumler.map { it.beatsPerMinute }.average().toLong()
        val sonOlcum = tumOlcumler.maxByOrNull { it.time }?.beatsPerMinute
        return Pair(ortalama, sonOlcum)
    }

    // --- UYKU (son 24 saat) ---
    private suspend fun readLastNightSleep(): String {
        val now = Instant.now()
        val yesterday = now.minus(Duration.ofHours(24))

        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(yesterday, now)
            )
        )

        if (response.records.isEmpty()) return "Veri yok"

        var toplamSure = Duration.ZERO
        for (record in response.records) {
            toplamSure = toplamSure.plus(Duration.between(record.startTime, record.endTime))
        }

        val saat = toplamSure.toHours()
        val dakika = toplamSure.toMinutes() % 60
        return "${saat} sa ${dakika} dk"
    }

    // --- EGZERSİZ (bugünkü oturumlar) ---
    private suspend fun readTodayExercise(): String {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(bugununBaslangici(), Instant.now())
            )
        )

        if (response.records.isEmpty()) return "Bugün egzersiz yok"

        var toplamSure = Duration.ZERO
        for (record in response.records) {
            toplamSure = toplamSure.plus(Duration.between(record.startTime, record.endTime))
        }

        val adet = response.records.size
        val dakika = toplamSure.toMinutes()
        return "$adet oturum, toplam $dakika dk"
    }

    // --- KULLANIM ERİŞİMİ İZNİ VAR MI KONTROL ET ---
    private fun kullanimErisimiIzniVarMi(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // --- KULLANICIYI İZİN VERMESİ İÇİN AYARLAR'A YÖNLENDİR ---
    private fun kullanimErisimiAyarlarinaGit() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    // --- BUGÜNKÜ TOPLAM EKRAN SÜRESİ (dakika) ---
    private fun readScreenTimeToday(): Long {
        if (!kullanimErisimiIzniVarMi()) return -1L

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val startTime = bugununBaslangici().toEpochMilli()
        val endTime = Instant.now().toEpochMilli()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        var toplamMs = 0L
        var sonAcilmaZamani: Long? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    sonAcilmaZamani = event.timeStamp
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (sonAcilmaZamani != null) {
                        toplamMs += event.timeStamp - sonAcilmaZamani
                        sonAcilmaZamani = null
                    }
                }
            }
        }

        return toplamMs / 1000 / 60
    }

    // --- GECE SAATLERİNDE EKRANIN EN UZUN KAPALI KALDIĞI ARALIK (başlangıç-bitiş saatiyle) ---
    private fun estimateSleepFromScreenOff(): String {
        if (!kullanimErisimiIzniVarMi()) return "İzin gerekli"

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val zoneId = ZoneId.systemDefault()

        // Dünkü öğlen 12:00'den bugünkü şu ana kadar bak (geniş pencere, gece mutlaka içinde kalsın)
        val today = LocalDate.now(zoneId)
        val startTime = today.minusDays(1).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
        val endTime = Instant.now().toEpochMilli()

        val events = usageStatsManager.queryEvents(startTime, endTime)

        var enUzunSure = 0L
        var enUzunBaslangic: Long? = null
        var enUzunBitis: Long? = null

        var suankiKapanmaZamani: Long? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    // Ekran kapandı, bu anı not al
                    suankiKapanmaZamani = event.timeStamp
                }

                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    // Ekran açıldı, eğer daha önce bir kapanma anı kaydettiysek süreyi hesapla
                    if (suankiKapanmaZamani != null) {
                        val sure = event.timeStamp - suankiKapanmaZamani!!
                        if (sure > enUzunSure) {
                            enUzunSure = sure
                            enUzunBaslangic = suankiKapanmaZamani
                            enUzunBitis = event.timeStamp
                        }
                        suankiKapanmaZamani = null
                    }
                }
            }
        }

        if (enUzunBaslangic == null || enUzunBitis == null) {
            return "Veri yetersiz"
        }

        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val baslangicSaat = Instant.ofEpochMilli(enUzunBaslangic!!).atZone(zoneId).format(formatter)
        val bitisSaat = Instant.ofEpochMilli(enUzunBitis!!).atZone(zoneId).format(formatter)

        val saat = enUzunSure / 1000 / 60 / 60
        val dakika = (enUzunSure / 1000 / 60) % 60

        return "$baslangicSaat - $bitisSaat (${saat} sa ${dakika} dk, tahmini)"
    }
}
@Composable
fun AnaEkran(
    adimSayisi: Long?,
    ortalamaNabiz: Long?,
    sonNabiz: Long?,
    uykuSuresi: String?,
    egzersizOzeti: String?,
    ekranSuresi: String?,
    tahminiUyku: String?,
    durumMesaji: String,
    onYenileTiklandi: () -> Unit,
    onIzinIste: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (ekranSuresi == "İzin gerekli") {
            Button(onClick = onIzinIste) {
                Text("Kullanım İzni Ver")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        VeriKarti(baslik = "Bugünkü Adım Sayısı", deger = adimSayisi?.toString() ?: "-")
        Spacer(modifier = Modifier.height(16.dp))

        VeriKarti(baslik = "Ortalama Nabız", deger = ortalamaNabiz?.let { "$it bpm" } ?: "Veri yok")
        Text(
            text = "Son ölçülen: ${sonNabiz?.let { "$it bpm" } ?: "-"}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        VeriKarti(baslik = "Uyku Süresi (son 24 saat)", deger = uykuSuresi ?: "-")
        if (tahminiUyku != null) {
            Text(text = tahminiUyku, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))

        VeriKarti(baslik = "Bugünkü Egzersiz", deger = egzersizOzeti ?: "-")
        Spacer(modifier = Modifier.height(16.dp))

        VeriKarti(baslik = "Ekran Süresi (bugün)", deger = ekranSuresi ?: "-")
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = durumMesaji, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onYenileTiklandi) {
            Text("Verileri Yenile")
        }
    }
}

@Composable
fun VeriKarti(baslik: String, deger: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = baslik, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = deger, style = MaterialTheme.typography.headlineMedium)
    }
}