package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LocationBackground = Color(0xFFF1F7F9)
private val LocationDarkText = Color(0xFF14263D)
private val LocationSecondaryText = Color(0xFF7890A2)
private val LocationTeal = Color(0xFF1D6679)
private val LocationGreen = Color(0xFF35AD6D)
private val LocationOrange = Color(0xFFF0A63A)
private val LocationRed = Color(0xFFD96849)

private data class RiskLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeter: Int,
    val active: Boolean = true
)

@Composable
fun RiskLocationScreen(
    patientName: String,
    onBack: () -> Unit = {}
) {
    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var locationAddedMessage by remember {
        mutableStateOf<String?>(null)
    }

    var savedLocations by remember {
        mutableStateOf(
            listOf(
                RiskLocation(
                    id = "1",
                    name = "Eski Mahalle",
                    latitude = 36.7800,
                    longitude = 31.4300,
                    radiusMeter = 250
                ),
                RiskLocation(
                    id = "2",
                    name = "Riskli Bölge",
                    latitude = 36.7842,
                    longitude = 31.4385,
                    radiusMeter = 150
                )
            )
        )
    }

    /*
     * Bu koordinatlar prototip amaçlı yerel verilerdir.
     * Gerçek GPS entegrasyonu yapıldığında cihaz konumundan alınacaktır.
     */
    val currentLatitude = 36.7824
    val currentLongitude = 31.4351

    val isInsideSafeArea = true

    val locationStatusText = if (isInsideSafeArea) {
        "Güvenli bölgede"
    } else {
        "Riskli bölgeye yakın"
    }

    val locationStatusColor = if (isInsideSafeArea) {
        LocationGreen
    } else {
        LocationRed
    }

    Scaffold(
        containerColor = LocationBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                },
                containerColor = LocationTeal,
                contentColor = Color.White
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 100.dp
                )
        ) {
            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Hasta detayına dön")
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Text(
                text = "Konum Takibi",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LocationDarkText
            )

            Text(
                text = "$patientName için son bilinen konum ve Geofence durumu.",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = LocationSecondaryText
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            PatientLocationSummaryCard(
                patientName = patientName,
                statusText = locationStatusText,
                statusColor = locationStatusColor
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            PrototypeMapCard(
                latitude = currentLatitude,
                longitude = currentLongitude,
                isInsideSafeArea = isInsideSafeArea
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            LocationInformationCard(
                latitude = currentLatitude,
                longitude = currentLongitude,
                statusText = locationStatusText,
                statusColor = locationStatusColor
            )

            if (locationAddedMessage != null) {
                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFDDF5E7)
                ) {
                    Text(
                        text = locationAddedMessage.orEmpty(),
                        modifier = Modifier.padding(15.dp),
                        fontSize = 13.sp,
                        color = Color(0xFF24784A)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Text(
                text = "Tanımlanan Riskli Alanlar",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = LocationDarkText
            )

            Text(
                text = "Bu alanlara giriş algılandığında sistem uyarı oluşturabilir.",
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = LocationSecondaryText
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            if (savedLocations.isEmpty()) {
                EmptyLocationCard()
            } else {
                savedLocations.forEach { location ->
                    RiskLocationCard(
                        location = location,
                        onRemove = {
                            savedLocations =
                                savedLocations.filterNot {
                                    it.id == location.id
                                }
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            PrivacyInformationCard()
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
            },
            title = {
                Text(
                    text = "Yeni Riskli Alan Ekle",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "Mevcut prototip konumu riskli alan olarak kaydedilecek. Bu işlem yalnızca yerel gösterim amacı taşır."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLocation = RiskLocation(
                            id = System.currentTimeMillis()
                                .toString(),
                            name = "Yeni Riskli Alan",
                            latitude = currentLatitude,
                            longitude = currentLongitude,
                            radiusMeter = 200
                        )

                        savedLocations =
                            savedLocations + newLocation

                        locationAddedMessage =
                            "Yeni riskli alan yerel olarak kaydedildi."

                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocationTeal
                    )
                ) {
                    Text("Alanı Kaydet")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                    }
                ) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun PatientLocationSummaryCard(
    patientName: String,
    statusText: String,
    statusColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = Color(0xFFD8F0F2)
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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocationDarkText
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patientName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocationDarkText
                )

                Text(
                    text = "Son konum güncellemesi: Şimdi",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
                    color = LocationSecondaryText
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun PrototypeMapCard(
    latitude: Double,
    longitude: Double,
    isInsideSafeArea: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Son Bilinen Konum",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocationDarkText
                    )

                    Text(
                        text = "Yerel GPS prototip görünümü",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        color = LocationSecondaryText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFE7F3F5)
                ) {
                    Text(
                        text = "PROTOTİP",
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocationTeal
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFFDDECEF)),
                contentAlignment = Alignment.Center
            ) {
                MapDecorationLines()

                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = CircleShape,
                    color = if (isInsideSafeArea) {
                        LocationGreen.copy(alpha = 0.17f)
                    } else {
                        LocationRed.copy(alpha = 0.17f)
                    }
                ) {}

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📍",
                        fontSize = 42.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White
                    ) {
                        Text(
                            text = "Hasta konumu",
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LocationDarkText
                        )
                    }
                }

                Text(
                    text =
                        "%.4f, %.4f".format(
                            latitude,
                            longitude
                        ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    fontSize = 10.sp,
                    color = LocationSecondaryText
                )
            }
        }
    }
}

@Composable
private fun MapDecorationLines() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.SpaceEvenly
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Color.White.copy(alpha = 0.55f)
                    )
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(
                        Color.White.copy(alpha = 0.55f)
                    )
            )
        }
    }
}

@Composable
private fun LocationInformationCard(
    latitude: Double,
    longitude: Double,
    statusText: String,
    statusColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            Text(
                text = "Konum Bilgileri",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = LocationDarkText
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            LocationInformationRow(
                title = "Enlem",
                value = "%.4f".format(latitude)
            )

            LocationInformationRow(
                title = "Boylam",
                value = "%.4f".format(longitude)
            )

            LocationInformationRow(
                title = "Son güncelleme",
                value = "Şimdi"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Geofence durumu",
                    fontSize = 13.sp,
                    color = LocationSecondaryText
                )

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun LocationInformationRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = LocationSecondaryText
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LocationDarkText
        )
    }
}

@Composable
private fun RiskLocationCard(
    location: RiskLocation,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = Color(0xFFFFE7DF)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = location.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocationDarkText
                )

                Text(
                    text =
                        "${location.radiusMeter} metre yarıçap · Geofence aktif",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
                    color = if (location.active) {
                        LocationGreen
                    } else {
                        LocationSecondaryText
                    }
                )

                Text(
                    text =
                        "%.4f, %.4f".format(
                            location.latitude,
                            location.longitude
                        ),
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 10.sp,
                    color = LocationSecondaryText
                )
            }

            Text(
                text = "Sil",
                modifier = Modifier
                    .clickable {
                        onRemove()
                    }
                    .padding(8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LocationRed
            )
        }
    }
}

@Composable
private fun EmptyLocationCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = "📍",
                fontSize = 31.sp
            )

            Text(
                text = "Riskli alan bulunmuyor",
                modifier = Modifier.padding(top = 9.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LocationDarkText
            )

            Text(
                text =
                    "Yeni bir alan eklemek için sağ alttaki + düğmesini kullanabilirsiniz.",
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                color = LocationSecondaryText
            )
        }
    }
}

@Composable
private fun PrivacyInformationCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE7F3F5)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔒 Konum Gizliliği",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LocationDarkText
            )

            Text(
                text =
                    "Bu prototipte konum ve riskli alan bilgileri yalnızca cihaz üzerinde gösterilir. Arka planda kesintisiz GPS takibi yapılmaz. Geofence sistemi yalnızca tanımlanan alanlara giriş ve çıkış durumunu değerlendirmek amacıyla tasarlanmıştır.",
                modifier = Modifier.padding(top = 7.dp),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = LocationSecondaryText
            )
        }
    }
}