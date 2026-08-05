package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DetailBackground = Color(0xFFF1F7F9)
private val DetailDarkText = Color(0xFF14263D)
private val DetailSecondaryText = Color(0xFF7890A2)
private val DetailTeal = Color(0xFF1D6679)

@Composable
fun PatientDetailScreen(
    patientName: String,
    stepCount: Long? = null,
    averageHeartRate: Long? = null,
    latestHeartRate: Long? = null,
    sleepDuration: String? = null,
    exerciseSummary: String? = null,
    onBack: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DetailBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Hasta listesine dön")
            }

            Spacer(modifier = Modifier.height(22.dp))

            PatientDetailHeader(
                patientName = patientName
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Bugünkü Sağlık Özeti",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DetailDarkText
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PatientHealthCard(
                    modifier = Modifier.weight(1f),
                    symbol = "❤️",
                    title = "ORT. NABIZ",
                    value = averageHeartRate?.toString() ?: "--",
                    unit = "bpm",
                    backgroundColor = Color(0xFFFFDED5)
                )

                PatientHealthCard(
                    modifier = Modifier.weight(1f),
                    symbol = "👣",
                    title = "ADIM",
                    value = stepCount?.let {
                        formatPatientNumber(it)
                    } ?: "--",
                    unit = "adım",
                    backgroundColor = Color(0xFFD7F0F6)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            PatientInformationCard(
                title = "Nabız Bilgileri",
                symbol = "❤️",
                rows = listOf(
                    "Ortalama nabız" to (
                            averageHeartRate?.let { "$it bpm" }
                                ?: "Veri bulunamadı"
                            ),
                    "Son ölçüm" to (
                            latestHeartRate?.let { "$it bpm" }
                                ?: "Veri bulunamadı"
                            ),
                    "Durum" to heartRateStatus(latestHeartRate)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            PatientInformationCard(
                title = "Uyku Özeti",
                symbol = "😴",
                rows = listOf(
                    "Son uyku süresi" to (
                            sleepDuration
                                ?.takeUnless { it == "Veri yok" }
                                ?: "Veri bulunamadı"
                            ),
                    "Hedef" to "En az 7 saat",
                    "Kayıt durumu" to if (
                        sleepDuration.isNullOrBlank() ||
                        sleepDuration == "Veri yok"
                    ) {
                        "Kayıt bulunamadı"
                    } else {
                        "Kayıt mevcut"
                    }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            PatientInformationCard(
                title = "Aktivite Özeti",
                symbol = "🏃",
                rows = listOf(
                    "Günlük adım" to (
                            stepCount?.let {
                                "${formatPatientNumber(it)} adım"
                            } ?: "Veri bulunamadı"
                            ),
                    "Adım hedefi" to "8.000 adım",
                    "Egzersiz" to (
                            exerciseSummary
                                ?.takeUnless {
                                    it == "Bugün egzersiz yok"
                                }
                                ?: "Egzersiz kaydı bulunamadı"
                            )
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            RiskStatusCard(
                latestHeartRate = latestHeartRate,
                sleepDuration = sleepDuration
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PatientDetailHeader(
    patientName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = DetailTeal
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patientName
                        .trim()
                        .firstOrNull()
                        ?.uppercase()
                        ?: "H",
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(15.dp))

        Column {
            Text(
                text = patientName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DetailDarkText
            )

            Text(
                text = "● Stabil",
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 14.sp,
                color = Color(0xFF2FA866)
            )

            Text(
                text = "Son güncelleme: 2 dk önce",
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
                color = DetailSecondaryText
            )
        }
    }
}

@Composable
private fun PatientHealthCard(
    modifier: Modifier = Modifier,
    symbol: String,
    title: String,
    value: String,
    unit: String,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier.height(155.dp),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = DetailSecondaryText
                )

                Text(
                    text = symbol,
                    fontSize = 21.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = value,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = DetailDarkText
            )

            Text(
                text = unit,
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 12.sp,
                color = DetailSecondaryText
            )
        }
    }
}

@Composable
private fun PatientInformationCard(
    title: String,
    symbol: String,
    rows: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = symbol,
                    fontSize = 21.sp
                )

                Spacer(modifier = Modifier.width(9.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailDarkText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = row.first,
                        fontSize = 13.sp,
                        color = DetailSecondaryText
                    )

                    Text(
                        text = row.second,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailDarkText
                    )
                }

                if (index < rows.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun RiskStatusCard(
    latestHeartRate: Long?,
    sleepDuration: String?
) {
    val hasData = latestHeartRate != null ||
            (
                    !sleepDuration.isNullOrBlank() &&
                            sleepDuration != "Veri yok"
                    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (hasData) {
            Color(0xFFE3F5EA)
        } else {
            Color(0xFFF1F4F5)
        }
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            Text(
                text = "Genel Değerlendirme",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DetailDarkText
            )

            Text(
                text = if (hasData) {
                    "Mevcut sağlık verilerinde acil müdahale gerektiren belirgin bir durum görünmüyor."
                } else {
                    "Değerlendirme için yeterli güncel sağlık verisi bulunmuyor."
                },
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = DetailSecondaryText
            )
        }
    }
}

private fun heartRateStatus(
    latestHeartRate: Long?
): String {
    return when {
        latestHeartRate == null -> "Veri bulunamadı"
        latestHeartRate < 60 -> "Düşük"
        latestHeartRate <= 100 -> "Normal aralık"
        else -> "Dikkat gerektiriyor"
    }
}

private fun formatPatientNumber(
    value: Long
): String {
    return "%,d"
        .format(value)
        .replace(',', '.')
}
