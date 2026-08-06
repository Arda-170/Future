package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PatientItem(
    val name: String,
    val status: String,
    val lastUpdate: String,
    val isLocalPatient: Boolean = false
)

@Composable
fun MonitoringHomeScreen(
    userName: String,
    userRole: UserRole,
    localPatientName: String,
    localStepCount: Long?,
    localHeartRate: Long?,
    localSleepDuration: String?,
    onPatientClick: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val displayName = userName
        .trim()
        .substringBefore(" ")
        .ifBlank { "Kullanıcı" }

    val userInitial = displayName
        .firstOrNull()
        ?.uppercase()
        ?: "K"

    /*
     * Local hastanın durumu gerçek nabız verisine göre belirlenir.
     * Bu eşikler yalnızca prototip gösterim amacı taşır.
     */
    val localPatientStatus = when {
        localHeartRate == null -> "Stabil"
        localHeartRate > 110L -> "Riskli"
        localHeartRate > 100L -> "Dikkat"
        else -> "Stabil"
    }

    val hasLocalHealthData =
        localStepCount != null ||
                localHeartRate != null ||
                !localSleepDuration.isNullOrBlank() &&
                localSleepDuration != "Veri yok"

    val localPatientUpdate = if (hasLocalHealthData) {
        "Health Connect verisi · Şimdi"
    } else {
        "Sağlık verisi bulunamadı"
    }

    /*
     * Birinci hasta gerçek local veriyi kullanır.
     * Diğer iki hasta çoklu hasta senaryosu için demo veridir.
     */
    val patients = listOf(
        PatientItem(
            name = localPatientName
                .trim()
                .ifBlank { "Mehmet Çelik" },
            status = localPatientStatus,
            lastUpdate = localPatientUpdate,
            isLocalPatient = true
        ),
        PatientItem(
            name = "Ahmet Yılmaz",
            status = "Dikkat",
            lastUpdate = "18 dk önce"
        ),
        PatientItem(
            name = "Ayşe Demir",
            status = "Riskli",
            lastUpdate = "1 saat önce"
        )
    )

    val screenTitle = when (userRole) {
        UserRole.DOCTOR -> "Takip Edilen Hastalar"
        UserRole.RELATIVE -> "Yakınlarım"
        else -> "Sağlık Takibi"
    }

    val screenDescription = when (userRole) {
        UserRole.DOCTOR ->
            "Hastaların güncel sağlık durumlarını görüntüleyin."

        UserRole.RELATIVE ->
            "Sağlık verilerini paylaşan yakınlarınızı görüntüleyin."

        else ->
            "Güncel sağlık bilgilerini görüntüleyin."
    }

    Scaffold(
        containerColor = Color(0xFFF1F7F9)
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonitoringHeader(
                    displayName = displayName,
                    userInitial = userInitial,
                    onOpenNotifications = onOpenNotifications,
                    onOpenProfile = onOpenProfile
                )
            }

            item {
                Column {
                    Text(
                        text = screenTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14263D)
                    )

                    Text(
                        text = screenDescription,
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF7890A2)
                    )
                }
            }

            items(
                items = patients,
                key = { patient ->
                    patient.name
                }
            ) { patient ->
                PatientCard(
                    patient = patient,
                    onClick = {
                        onPatientClick(patient.name)
                    }
                )
            }
        }
    }
}

@Composable
private fun MonitoringHeader(
    displayName: String,
    userInitial: String,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "İyi günler!",
                fontSize = 20.sp,
                color = Color(0xFF7890A2)
            )

            Text(
                text = displayName,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14263D)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        onOpenNotifications()
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔔",
                        fontSize = 21.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        onOpenProfile()
                    },
                shape = CircleShape,
                color = Color(0xFF1D6679),
                shadowElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient: PatientItem,
    onClick: () -> Unit
) {
    val statusColor = when (patient.status) {
        "Stabil" -> Color(0xFF35AD6D)
        "Dikkat" -> Color(0xFFF0A63A)
        else -> Color(0xFFD96849)
    }

    val avatarBackground = if (patient.isLocalPatient) {
        Color(0xFFDDF5E7)
    } else {
        Color(0xFFD8F0F2)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = avatarBackground
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = patient.name
                            .trim()
                            .firstOrNull()
                            ?.uppercase()
                            ?: "H",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14263D)
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = patient.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14263D)
                    )

                    if (patient.isLocalPatient) {
                        Spacer(
                            modifier = Modifier.width(7.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDDF5E7)
                        ) {
                            Text(
                                text = "CANLI",
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2F8A58)
                            )
                        }
                    }
                }

                Text(
                    text = patient.lastUpdate,
                    modifier = Modifier.padding(top = 5.dp),
                    color = Color(0xFF7890A2),
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = patient.status,
                color = statusColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}