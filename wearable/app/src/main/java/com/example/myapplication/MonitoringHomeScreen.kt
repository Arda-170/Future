package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Scaffold

private data class PatientItem(
    val name: String,
    val status: String,
    val lastUpdate: String
)

@Composable
fun MonitoringHomeScreen(
    userName: String,
    userRole: UserRole,
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

    val patients = listOf(
        PatientItem(
            name = "Mehmet Çelik",
            status = "Stabil",
            lastUpdate = "2 dk önce"
        ),
        PatientItem(
            name = "Ahmet Yılmaz",
            status = "Dikkat",
            lastUpdate = "18 dk önce"
        ),
        PatientItem(
            name = "Ayşe Demir",
            status = "Riskli",
            lastUpdate = "Şimdi"
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

            items(patients) { patient ->
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
    Column {
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = Color(0xFFD8F0F2)
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF14263D)
                )

                Text(
                    text = patient.lastUpdate,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0xFF7890A2),
                    fontSize = 12.sp
                )
            }

            Text(
                text = patient.status,
                color = statusColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}