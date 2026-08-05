package com.example.myapplication   // <-- kendi paket adınla aynı olsun

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.time.Instant

// Backend'in "health_samples" tablosunun beklediği tam şekil.
// Alan isimleri api-sozlesmesi.md'deki isimlerle BİREBİR aynı olmalı.
@Serializable
data class HealthSample(
    val client_uid: String,
    val metric: String,
    val value: Double,
    val unit: String? = null,
    val start_time: String,
    val end_time: String? = null,
    val source: String = "health_connect"
)

// api-sozlesmesi.md Bölüm 2.2'deki algoritmanın birebir aynısı.
// Aynı veri tekrar okunduğunda AYNI client_uid üretilmeli,
// yoksa backend'de çift kayıt oluşur.
fun clientUid(metric: String, startTime: Instant, source: String): String {
    val raw = "$metric|${startTime.toEpochMilli()}|$source"
    return MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(32)
}

// Kolay kullanım için: bir HealthSample üretmemizi sağlayan yardımcı fonksiyon
fun buildHealthSample(
    metric: String,
    value: Double,
    startTime: Instant,
    endTime: Instant? = null,
    unit: String? = null
): HealthSample {
    return HealthSample(
        client_uid = clientUid(metric, startTime, "health_connect"),
        metric = metric,
        value = value,
        unit = unit,
        start_time = startTime.toString(),
        end_time = endTime?.toString(),
        source = "health_connect"
    )
}