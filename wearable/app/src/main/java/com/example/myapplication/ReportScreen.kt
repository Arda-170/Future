package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private val ReportBackground = Color(0xFFF1F7F9)
private val ReportDarkText = Color(0xFF14263D)
private val ReportSecondaryText = Color(0xFF7890A2)
private val ReportTeal = Color(0xFF1D6679)

@Composable
fun ReportScreen(
    adimSayisi: Long? = null,
    ortalamaNabiz: Long? = null,
    sonNabiz: Long? = null,
    uykuSuresi: String? = null,
    egzersizOzeti: String? = null,
    heartRatePoints: List<HeartRatePoint> = emptyList(),
    onBack: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ReportBackground
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
                Text("← Geri")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sağlık Raporu",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ReportDarkText
            )

            Text(
                text = "Bugünkü genel görünüm",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 15.sp,
                color = ReportSecondaryText
            )

            Spacer(modifier = Modifier.height(24.dp))

            ReportSummaryCards(
                stepCount = adimSayisi,
                averageHeartRate = ortalamaNabiz
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeeklyProgressCard()

            Spacer(modifier = Modifier.height(16.dp))

            HeartRateReportCard(
                averageHeartRate = ortalamaNabiz,
                latestHeartRate = sonNabiz,
                heartRatePoints = heartRatePoints
            )

            Spacer(modifier = Modifier.height(16.dp))

            SleepReportCard(
                sleepDuration = uykuSuresi
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExerciseReportCard(
                exerciseSummary = egzersizOzeti
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bu rapor bilgilendirme amacı taşır ve tıbbi değerlendirme yerine geçmez.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = ReportSecondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEFF6F7)
            ) {
                Text(
                    text = "Saatlik nabız grafiği ve haftalık aktivite geçmişi için geçmiş veri toplanması gerekiyor — bu özellik yakında eklenecek.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = ReportSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReportSummaryCards(
    stepCount: Long?,
    averageHeartRate: Long?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReportSmallCard(
            modifier = Modifier.weight(1f),
            title = "BUGÜNKÜ ADIM",
            value = stepCount?.let { formatReportNumber(it) } ?: "--",
            unit = "adım",
            backgroundColor = Color(0xFFD6EFF6)
        )

        ReportSmallCard(
            modifier = Modifier.weight(1f),
            title = "ORT. NABIZ",
            value = averageHeartRate?.toString() ?: "--",
            unit = "bpm",
            backgroundColor = Color(0xFFFFE2DA)
        )
    }
}

private fun formatReportNumber(value: Long): String {
    return "%,d".format(value).replace(',', '.')
}

@Composable
private fun ReportSmallCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = ReportSecondaryText
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ReportDarkText
            )

            Text(
                text = unit,
                fontSize = 12.sp,
                color = ReportSecondaryText
            )
        }
    }
}

@Composable
private fun WeeklyProgressCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Haftalık Aktivite",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ReportDarkText
            )

            Text(
                text = "Günlük adım hedefi ilerlemesi",
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp,
                color = ReportSecondaryText
            )

            Spacer(modifier = Modifier.height(20.dp))

            val values = listOf(
                "Pzt" to 0.70f,
                "Sal" to 0.55f,
                "Çar" to 0.85f,
                "Per" to 0.60f,
                "Cum" to 0.92f,
                "Cmt" to 0.76f,
                "Paz" to 0.48f
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                values.forEach { item ->
                    ReportBar(
                        day = item.first,
                        progress = item.second
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportBar(
    day: String,
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(110.dp)
                .background(
                    color = Color(0xFFDCECEF),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .background(
                        color = ReportTeal,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = day,
            fontSize = 11.sp,
            color = ReportSecondaryText
        )
    }
}

@Composable
private fun HeartRateReportCard(
    averageHeartRate: Long?,
    latestHeartRate: Long?,
    heartRatePoints: List<HeartRatePoint>
) {

    val heartRateValues = heartRatePoints.map { it.bpm }
    val timeLabels = heartRatePoints.map { it.timeLabel }
    val hasChartData = heartRateValues.size >= 2

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nabız Grafiği",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReportDarkText
                    )

                    Text(
                        text = "Gün içindeki nabız ölçümleri",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        color = ReportSecondaryText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFE2DA)
                ) {
                    Text(
                        text = averageHeartRate?.let { "Ort. $it bpm" } ?: "Ort. -- bpm",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC85F4A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (hasChartData) {

                HeartRateLineChart(
                    values = heartRateValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = timeLabels.first(),
                        fontSize = 11.sp,
                        color = ReportSecondaryText
                    )

                    Text(
                        text = timeLabels[timeLabels.size / 2],
                        fontSize = 11.sp,
                        color = ReportSecondaryText
                    )

                    Text(
                        text = timeLabels.last(),
                        fontSize = 11.sp,
                        color = ReportSecondaryText
                    )
                }

            } else {

                Text(
                    text = "Grafik için yeterli veri bulunamadı.",
                    color = ReportSecondaryText,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

            }

            Spacer(modifier = Modifier.height(18.dp))

            if (heartRatePoints.size >= 2) {
                val highestPoint = heartRatePoints.maxByOrNull { it.bpm }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF1EC)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Dikkat çeken ölçüm",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC85F4A)
                        )

                        Text(
                            text = highestPoint?.let {
                                "${it.timeLabel} saatinde ${it.bpm.toInt()} bpm ölçüldü."
                            } ?: "Yeterli ölçüm bulunamadı.",
                            modifier = Modifier.padding(top = 5.dp),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = ReportSecondaryText
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F7F9)
                ) {
                    Text(
                        text = "Nabız değerlendirmesi için daha fazla ölçüm gerekiyor.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = ReportSecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ReportInformationRow(
                title = "Son ölçüm",
                value = latestHeartRate?.let { "$it bpm" } ?: "Veri yok"
            )

            ReportInformationRow(
                title = "Ortalama",
                value = averageHeartRate?.let { "$it bpm" } ?: "Veri yok"
            )

            ReportInformationRow(
                title = "Durum",
                value = when {
                    latestHeartRate == null -> "Veri yok"
                    latestHeartRate < 60 -> "Düşük"
                    latestHeartRate <= 100 -> "Normal"
                    else -> "Yüksek"
                }
            )

        }
    }
}

@Composable
private fun HeartRateLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) {
        Text(
            text = "Grafik için yeterli veri bulunamadı.",
            color = ReportSecondaryText
        )
        return
    }

    val minimumValue = 50f
    val maximumValue = 120f
    val normalLineColor = Color(0xFF1D6679)
    val highPointColor = Color(0xFFD96849)
    val gridColor = Color(0xFFDCECEF)

    Canvas(
        modifier = modifier
    ) {
        val graphLeft = 8.dp.toPx()
        val graphRight = size.width - 8.dp.toPx()
        val graphTop = 10.dp.toPx()
        val graphBottom = size.height - 10.dp.toPx()

        val graphWidth = graphRight - graphLeft
        val graphHeight = graphBottom - graphTop

        repeat(4) { index ->
            val y = graphTop + (graphHeight / 3f) * index

            drawLine(
                color = gridColor,
                start = Offset(graphLeft, y),
                end = Offset(graphRight, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val points = values.mapIndexed { index, value ->
            val x = graphLeft +
                    (index.toFloat() / values.lastIndex.toFloat()) * graphWidth

            val normalizedValue =
                ((value - minimumValue) / (maximumValue - minimumValue))
                    .coerceIn(0f, 1f)

            val y = graphBottom - normalizedValue * graphHeight

            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)

            for (index in 1 until points.size) {
                lineTo(points[index].x, points[index].y)
            }
        }

        drawPath(
            path = path,
            color = normalLineColor,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        points.forEachIndexed { index, point ->
            val isHighValue = values[index] >= 100f

            drawCircle(
                color = if (isHighValue) {
                    highPointColor
                } else {
                    normalLineColor
                },
                radius = if (isHighValue) {
                    6.dp.toPx()
                } else {
                    4.dp.toPx()
                },
                center = point
            )

            if (isHighValue) {
                drawCircle(
                    color = highPointColor.copy(alpha = 0.20f),
                    radius = 12.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
private fun SleepReportCard(
    sleepDuration: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Uyku Özeti",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ReportDarkText
            )

            Spacer(modifier = Modifier.height(14.dp))

            ReportInformationRow(
                title = "Son uyku",
                value = sleepDuration ?: "Veri yok"
            )

            ReportInformationRow(
                title = "Durum",
                value = if (
                    sleepDuration == null || sleepDuration == "Veri yok"
                ) {
                    "Kayıt bulunamadı"
                } else {
                    "Kayıt mevcut"
                }
            )
        }
    }
}

@Composable
private fun ReportInformationRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = ReportSecondaryText
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ReportDarkText
        )
    }
}

@Composable
private fun ExerciseReportCard(
    exerciseSummary: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Egzersiz Özeti",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ReportDarkText
            )

            Text(
                text = exerciseSummary ?: "Veri bulunamadı",
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 14.sp,
                color = ReportSecondaryText
            )
        }
    }
}