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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import java.time.format.DateTimeFormatter
import androidx.compose.runtime.mutableIntStateOf
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.providers.builtin.Email
data class HeartRatePoint(
    val timeLabel: String,
    val bpm: Float
)

data class HeartRateResult(
    val average: Long?,
    val latest: Long?,
    val points: List<HeartRatePoint>
)


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
    private var heartRatePointsState =
        mutableStateOf<List<HeartRatePoint>>(emptyList())
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

        Log.d("TEST", "onCreate çalıştı")

        checkHealthConnectDurumu()

        val sharedPreferences =
            getSharedPreferences("UserData", MODE_PRIVATE)

        lifecycleScope.launch {
            try {
                if (supabase.auth.currentUserOrNull() == null) {
                    supabase.auth.signInWith(Email) {
                        email = "omersahin68n@gmail.com"
                        password = "Kk.350642"
                    }
                    Log.d("Supabase", "Giriş yapıldı: ${supabase.auth.currentUserOrNull()?.id}")
                } else {
                    Log.d("Supabase", "Zaten giriş yapılmış: ${supabase.auth.currentUserOrNull()?.id}")
                }

                // Sağlık verisi rızası — health_samples insert'i için RLS bunu şart koşuyor
                rizaKaydiEkle()

            } catch (e: Exception) {
                Log.e("Supabase", "Giriş hatası: ${e.message}")
            }
        }


        setContent {
            MaterialTheme {
                val userStore = remember {
                    LocalUserStore(sharedPreferences)
                }

                var registrationError by remember {
                    mutableStateOf<String?>(null)
                }

                var loginError by remember {
                    mutableStateOf<String?>(null)
                }

                val appPreferences = remember {
                    getSharedPreferences(
                        "app_preferences",
                        MODE_PRIVATE
                    )
                }

                val onboardingCompleted = appPreferences.getBoolean(
                    "onboarding_completed",
                    false
                )

                val kvkkAccepted = appPreferences.getBoolean(
                    "kvkk_accepted",
                    false
                )

                var currentUserName by remember {
                    mutableStateOf(
                        sharedPreferences.getString(
                            "user_name",
                            ""
                        ) ?: ""
                    )
                }

                var currentUserEmail by remember {
                    mutableStateOf(
                        sharedPreferences.getString(
                            "user_email",
                            ""
                        ) ?: ""
                    )
                }

                var currentUserRole by remember {
                    mutableStateOf(UserRole.PATIENT)
                }

                var currentScreen by remember {
                    mutableStateOf(
                        if (!onboardingCompleted) {
                            "onboarding"
                        } else if (!kvkkAccepted) {
                            "consent"
                        } else {
                            "login"
                        }
                    )
                }

                var userPoints by remember {
                    mutableIntStateOf(
                        sharedPreferences.getInt(
                            "user_points",
                            15
                        )
                    )
                }

                var completedTaskCount by remember {
                    mutableIntStateOf(
                        sharedPreferences.getInt(
                            "completed_task_count",
                            2
                        )
                    )
                }

                var purchasedRewards by remember {
                    mutableStateOf(
                        sharedPreferences
                            .getStringSet(
                                "purchased_rewards",
                                emptySet()
                            )
                            ?.toList()
                            ?: emptyList()
                    )
                }

                val notificationPreferences = remember {
                    getSharedPreferences(
                        "notification_preferences",
                        MODE_PRIVATE
                    )
                }

                var hasUnreadNotifications by remember {
                    mutableStateOf(
                        notificationPreferences.getBoolean(
                            "has_unread_notifications",
                            true
                        )
                    )
                }

                var selectedPatientName by remember {
                    mutableStateOf("")
                }

                when (currentScreen) {

                    "register" -> {
                        RegisterScreen(
                            registrationError = registrationError,
                            onRegister = { fullName, email, password, role ->
                                val errorMessage = userStore.register(
                                    fullName = fullName,
                                    email = email,
                                    password = password,
                                    role = role
                                )

                                if (errorMessage == null) {
                                    registrationError = null
                                    currentScreen = "login"
                                } else {
                                    registrationError = errorMessage
                                }
                            },
                            onBack = {
                                registrationError = null
                                currentScreen = "login"
                            }
                        )
                    }

                    "login" -> {
                        LoginScreen(
                            loginError = loginError,
                            onLogin = { email, password ->
                                val authenticatedUser = userStore.authenticate(
                                    email = email,
                                    password = password
                                )

                                if (authenticatedUser == null) {
                                    loginError = "E-posta veya şifre hatalı."
                                } else {
                                    loginError = null

                                    currentUserName = authenticatedUser.fullName
                                    currentUserEmail = authenticatedUser.email
                                    currentUserRole = authenticatedUser.role

                                    sharedPreferences
                                        .edit()
                                        .putString(
                                            "active_user_email",
                                            authenticatedUser.email
                                        )
                                        .apply()

                                    currentScreen = when (authenticatedUser.role) {
                                        UserRole.PATIENT -> "home"

                                        UserRole.DOCTOR,
                                        UserRole.RELATIVE -> "monitoring_home"

                                        UserRole.ADMIN -> "admin_home"
                                    }
                                }
                            },
                            onRegister = {
                                loginError = null
                                currentScreen = "register"
                            }
                        )
                    }

                    "onboarding" -> {
                        OnboardingScreen(
                            onFinish = {
                                appPreferences
                                    .edit()
                                    .putBoolean("onboarding_completed", true)
                                    .apply()

                                currentScreen = "consent"
                            }
                        )
                    }

                    "consent" -> {
                        KvkkScreen(
                            onBack = {
                                currentScreen = "onboarding"
                            },
                            onAccept = {
                                appPreferences
                                    .edit()
                                    .putBoolean("kvkk_accepted", true)
                                    .apply()

                                checkHealthConnectDurumu()
                                currentScreen = "login"
                            }
                        )
                    }

                    "home" -> {
                        HomeScreen(
                            userName = currentUserName,
                            adimSayisi = stepCountState.value,
                            ortalamaNabiz = avgHeartRateState.value,
                            sonNabiz = latestHeartRateState.value,
                            uykuSuresi = sleepDurationState.value,
                            tahminiUyku = tahminiUykuState.value,

                            onOpenCrisis = {
                                currentScreen = "crisis"
                            },
                            onNavigate = { destination ->
                                currentScreen = destination
                            },
                            onOpenProfile = {
                                currentScreen = "profile"
                            },
                            onOpenNotifications = {
                                hasUnreadNotifications = false

                                notificationPreferences
                                    .edit()
                                    .putBoolean(
                                        "has_unread_notifications",
                                        false
                                    )
                                    .apply()

                                currentScreen = "notifications"
                            }
                        )
                    }

                    "crisis" -> {
                        CrisisScreen(
                            onBack = {
                                currentScreen = "home"
                            }
                        )
                    }

                    "report" -> {
                        ReportScreen(
                            adimSayisi = stepCountState.value,
                            ortalamaNabiz = avgHeartRateState.value,
                            sonNabiz = latestHeartRateState.value,
                            uykuSuresi = sleepDurationState.value,
                            egzersizOzeti = exerciseSummaryState.value,
                            heartRatePoints = heartRatePointsState.value,
                            onBack = {
                                currentScreen = "home"
                            }
                        )

                    }

                    "tasks" -> {
                        TasksScreen(
                            totalPoints = userPoints,
                            completedTaskCount = completedTaskCount,
                            currentSteps = stepCountState.value,
                            sleepDuration = sleepDurationState.value,
                            onPointsEarned = { earnedPoints ->
                                userPoints += earnedPoints
                                completedTaskCount += 1

                                sharedPreferences
                                    .edit()
                                    .putInt("user_points", userPoints)
                                    .putInt(
                                        "completed_task_count",
                                        completedTaskCount
                                    )
                                    .apply()
                            },
                            onBack = {
                                currentScreen = "home"
                            }
                        )
                    }

                    "market" -> {
                        MarketScreen(
                            userPoints = userPoints,
                            onRewardPurchased = { cost, rewardTitle ->
                                userPoints -= cost

                                if (!purchasedRewards.contains(rewardTitle)) {
                                    purchasedRewards =
                                        purchasedRewards + rewardTitle
                                }

                                sharedPreferences
                                    .edit()
                                    .putInt("user_points", userPoints)
                                    .putStringSet(
                                        "purchased_rewards",
                                        purchasedRewards.toSet()
                                    )
                                    .apply()
                            },
                            onBack = {
                                currentScreen = "home"
                            }
                        )
                    }

                    "profile" -> {
                        ProfileScreen(
                            userName = currentUserName,
                            userEmail = currentUserEmail,
                            totalPoints = userPoints,
                            completedTaskCount = completedTaskCount,
                            purchasedRewards = purchasedRewards,
                            onBack = {
                                currentScreen = "home"
                            },
                            onLogout = {
                                sharedPreferences
                                    .edit()
                                    .remove("active_user_email")
                                    .apply()

                                currentUserName = ""
                                currentUserEmail = ""
                                currentUserRole = UserRole.PATIENT

                                currentScreen = "login"
                            }
                        )
                    }

                    "notifications" -> {
                        NotificationsScreen(
                            hasUnreadNotifications = hasUnreadNotifications,
                            onBack = {
                                currentScreen = "home"
                            }
                        )
                    }

                    "monitoring_home" -> {
                        MonitoringHomeScreen(
                            userName = currentUserName,
                            userRole = currentUserRole,
                            onPatientClick = { patientName ->
                                selectedPatientName = patientName
                                currentScreen = "patient_detail"
                            },
                            onOpenNotifications = {
                                currentScreen = "role_notifications"
                            },
                            onOpenProfile = {
                                currentScreen = "role_profile"
                            }
                        )
                    }

                    "patient_detail" -> {
                        PatientDetailScreen(
                            patientName = selectedPatientName,
                            stepCount = stepCountState.value,
                            averageHeartRate = avgHeartRateState.value,
                            latestHeartRate = latestHeartRateState.value,
                            sleepDuration = sleepDurationState.value,
                            exerciseSummary = exerciseSummaryState.value,
                            onBack = {
                                currentScreen = "monitoring_home"
                            }
                        )
                    }

                    "admin_home" -> {
                        AdminHomeScreen(
                            userName = currentUserName,
                            onOpenProfile = {
                                currentScreen = "role_profile"
                            },
                            onOpenNotifications = {
                                currentScreen = "role_notifications"
                            }
                        )
                    }

                    "role_profile" -> {
                        RoleProfileScreen(
                            userName = currentUserName,
                            userEmail = currentUserEmail,
                            userRole = currentUserRole,
                            onBack = {
                                currentScreen = when (currentUserRole) {
                                    UserRole.DOCTOR,
                                    UserRole.RELATIVE -> "monitoring_home"

                                    UserRole.ADMIN -> "admin_home"

                                    UserRole.PATIENT -> "home"
                                }
                            },
                            onLogout = {
                                sharedPreferences
                                    .edit()
                                    .remove("active_user_email")
                                    .apply()

                                currentUserName = ""
                                currentUserEmail = ""
                                currentUserRole = UserRole.PATIENT
                                currentScreen = "login"
                            }
                        )
                    }

                }
            }
        }
    }
    // --- KVKK RIZA KAYDI (health_samples RLS için gerekli) ---
    private suspend fun rizaKaydiEkle() {
        try {
            supabase.from("consents").insert(
                mapOf("kind" to "health_data", "text_version" to "saglik-verisi-v1.0")
            )
            Log.d("Supabase", "Rıza kaydı eklendi")
        } catch (e: Exception) {
            // Zaten eklenmişse hata normal, tekrar tekrar eklemeye çalışmıyoruz diye
            Log.d("Supabase", "Rıza kaydı zaten var veya hata: ${e.message}")
        }
    }
    private fun sunucuyaGonder() {
        lifecycleScope.launch {
            try {
                val simdi = Instant.now()
                val samples = mutableListOf<HealthSample>()

                // Adım sayısı
                stepCountState.value?.let { adim ->
                    samples.add(
                        buildHealthSample(
                            metric = "steps",
                            value = adim.toDouble(),
                            startTime = bugununBaslangici(),
                            endTime = simdi,
                            unit = "count"
                        )
                    )
                }

                // Ortalama nabız
                avgHeartRateState.value?.let { nabiz ->
                    samples.add(
                        buildHealthSample(
                            metric = "heart_rate",
                            value = nabiz.toDouble(),
                            startTime = bugununBaslangici(),
                            endTime = simdi,
                            unit = "bpm"
                        )
                    )
                }

                if (samples.isEmpty()) {
                    Log.d("Supabase", "Gönderilecek veri yok")
                    return@launch
                }

                supabase.from("health_samples").upsert(samples) {
                    onConflict = "user_id,client_uid"
                    ignoreDuplicates = true
                }

                Log.d("Supabase", "${samples.size} kayıt gönderildi")

            } catch (e: Exception) {
                Log.e("Supabase", "Gönderim hatası: ${e.message}")
            }
        }
    }
    private fun checkHealthConnectDurumu() {
        Log.d("TEST", "checkHealthConnectDurumu çağrıldı")

        val status = HealthConnectClient.getSdkStatus(this)

        Log.d("TEST", "SDK Status = $status")

        when (status) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Log.d("TEST", "SDK_UNAVAILABLE")
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Log.d("TEST", "SDK_UPDATE_REQUIRED")
            }

            HealthConnectClient.SDK_AVAILABLE -> {
                Log.d("TEST", "SDK_AVAILABLE")
                checkAndRequestPermissions()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d("TEST", "checkAndRequestPermissions")

        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()

            Log.d("TEST", "Granted: $granted")

            if (granted.containsAll(permissions)) {
                Log.d("TEST", "İzinler tamam")
                tumVerileriCek()
            } else {
                Log.d("TEST", "İzin eksik")
                requestPermissionLauncher.launch(permissions)
            }
        }
    }

    // Tüm verileri sırayla çeken ana fonksiyon
    private fun tumVerileriCek() {
        // --- TÜM VERİLERİ SUPABASE'E GÖNDER ---
        Log.d("TEST", "tumVerileriCek başladı")

        lifecycleScope.launch {
            statusMessage.value = "Veriler çekiliyor..."

            Log.d("TEST", "1 - kaynaklariGoster çağrılacak")

            kaynaklariGoster()

            Log.d("TEST", "2 - kaynaklariGoster bitti")

            Log.d("TEST", "3 - readTodaySteps çağrılıyor")

            val adimSayisi = readTodaySteps()

            Log.d("TEST", "4 - readTodaySteps bitti: $adimSayisi")

            stepCountState.value = adimSayisi

            val heartRateResult = readTodayHeartRate()

            avgHeartRateState.value = heartRateResult.average
            latestHeartRateState.value = heartRateResult.latest
            heartRatePointsState.value = heartRateResult.points

            Log.d(
                "HealthConnect",
                "Ortalama nabız: ${heartRateResult.average}, " +
                        "Son nabız: ${heartRateResult.latest}, " +
                        "Ölçüm sayısı: ${heartRateResult.points.size}"
            )

            val uyku = readLastNightSleep()
            sleepDurationState.value = uyku
            Log.d("HealthConnect", "Uyku süresi: $uyku")

            val egzersiz = readTodayExercise()
            exerciseSummaryState.value = egzersiz
            Log.d("HealthConnect", "Egzersiz özeti: $egzersiz")

            // Ekran süresi
            val ekranDk = readScreenTimeToday()
            screenTimeState.value = if (ekranDk >= 0) "${ekranDk / 60} sa ${ekranDk % 60} dk" else "İzin gerekli"
            Log.d("HealthConnect", "Ekran süresi (dk): $ekranDk")

            // Eğer saatten uyku verisi gelmediyse, ekran kapalı süresinden tahmin et
            if (uyku == "Veri yok") {
                val tahminiDk = estimateSleepFromScreenOff()
                tahminiUykuState.value = if (tahminiDk >= 0) {
                    "${tahminiDk / 60} sa ${tahminiDk % 60} dk (tahmini, telefon kullanımına göre)"
                } else "İzin gerekli"
                Log.d("HealthConnect", "Tahmini uyku (dk): $tahminiDk")
            } else {
                tahminiUykuState.value = null
            }

            statusMessage.value = "Son güncelleme: şimdi"
            sunucuyaGonder()
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
    private suspend fun readTodayHeartRate(): HeartRateResult {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    bugununBaslangici(),
                    Instant.now()
                )
            )
        )

        val samples = response.records
            .flatMap { record -> record.samples }
            .sortedBy { sample -> sample.time }

        if (samples.isEmpty()) {
            return HeartRateResult(
                average = null,
                latest = null,
                points = emptyList()
            )
        }

        val average = samples
            .map { sample -> sample.beatsPerMinute }
            .average()
            .toLong()

        val latest = samples.last().beatsPerMinute

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val zoneId = ZoneId.systemDefault()

        val points = samples.map { sample ->
            HeartRatePoint(
                timeLabel = sample.time
                    .atZone(zoneId)
                    .format(formatter),
                bpm = sample.beatsPerMinute.toFloat()
            )
        }

        return HeartRateResult(
            average = average,
            latest = latest,
            points = points
        )
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
        Log.d("HealthConnect", "Sleep sorgu aralığı (24 saat): $yesterday - $now, bulunan kayıt sayısı: ${response.records.size}")

        // TEŞHİS: son 7 güne genişletilmiş sorgu
        val genisAralikBaslangic = now.minus(Duration.ofDays(7))
        val genisResponse = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(genisAralikBaslangic, now)
            )
        )
        Log.d("HealthConnect", "Sleep sorgu aralığı (7 gün): bulunan kayıt sayısı: ${genisResponse.records.size}")
        for (record in genisResponse.records) {
            Log.d("HealthConnect", "Kayıt: ${record.startTime} - ${record.endTime}")
        }

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

    // --- GECE SAATLERİNDE EKRANIN EN UZUN KAPALI KALDIĞI SÜRE (tahmini uyku) ---
    private fun estimateSleepFromScreenOff(): Long {
        if (!kullanimErisimiIzniVarMi()) return -1L

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val zoneId = ZoneId.systemDefault()

        val today = LocalDate.now(zoneId)
        val startTime = today.minusDays(1).atTime(21, 0).atZone(zoneId).toInstant().toEpochMilli()
        val endTime = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        var enUzunKapaliSure = 0L
        var kapanmaZamani: Long? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    kapanmaZamani = event.timeStamp
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    if (kapanmaZamani != null) {
                        val sure = event.timeStamp - kapanmaZamani
                        if (sure > enUzunKapaliSure) enUzunKapaliSure = sure
                        kapanmaZamani = null
                    }
                }
            }
        }

        return enUzunKapaliSure / 1000 / 60
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