package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

private val AdminBackground = Color(0xFFF1F7F9)
private val AdminDarkText = Color(0xFF14263D)
private val AdminSecondaryText = Color(0xFF7890A2)
private val AdminTeal = Color(0xFF1D6679)

@Composable
fun AdminHomeScreen(
    userName: String,
    onOpenProfile: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    val displayName = userName
        .trim()
        .substringBefore(" ")
        .ifBlank { "Kullanıcı" }

    val userInitial = displayName
        .firstOrNull()
        ?.uppercase()
        ?: "A"

    Scaffold(
        containerColor = AdminBackground
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
                AdminHeader(
                    displayName = displayName,
                    userInitial = userInitial,
                    onOpenProfile = onOpenProfile,
                    onOpenNotifications = onOpenNotifications
                )
            }

            item {
                Column {
                    Text(
                        text = "Admin Paneli",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminDarkText
                    )

                    Text(
                        text = "Kullanıcıları ve sistem durumunu yönetin.",
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 14.sp,
                        color = AdminSecondaryText
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        value = "24",
                        title = "Hasta"
                    )

                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        value = "6",
                        title = "Doktor"
                    )

                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        value = "18",
                        title = "Yakın"
                    )
                }
            }

            item {
                AdminOptionCard(
                    title = "Kullanıcı Yönetimi",
                    description = "Hasta, doktor ve hasta yakını hesaplarını görüntüle."
                )
            }

            item {
                AdminOptionCard(
                    title = "Rol ve Yetki Yönetimi",
                    description = "Kullanıcı rollerini ve erişim izinlerini düzenle."
                )
            }

            item {
                AdminOptionCard(
                    title = "Sistem Bildirimleri",
                    description = "Kritik uyarıları ve sistem mesajlarını incele."
                )
            }

            item {
                AdminOptionCard(
                    title = "Sağlık Verisi Bağlantıları",
                    description = "Health Connect bağlantı durumlarını kontrol et."
                )
            }

            item {
                AdminSystemStatusCard()
            }
        }
    }
}

@Composable
private fun AdminHeader(
    displayName: String,
    userInitial: String,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit
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
                color = AdminSecondaryText
            )

            Text(
                text = displayName,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = AdminDarkText
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
                color = AdminTeal,
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
private fun AdminStatCard(
    modifier: Modifier = Modifier,
    value: String,
    title: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 17.dp
            )
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AdminTeal
            )

            Text(
                text = title,
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 12.sp,
                color = AdminSecondaryText
            )
        }
    }
}

@Composable
private fun AdminOptionCard(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminDarkText
                )

                Text(
                    text = description,
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
                    color = AdminSecondaryText
                )
            }

            Text(
                text = "›",
                fontSize = 27.sp,
                color = AdminSecondaryText
            )
        }
    }
}

@Composable
private fun AdminSystemStatusCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE3F5EA)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Sistem Durumu",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AdminDarkText
            )

            Text(
                text = "● Tüm servisler çalışıyor",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2FA866)
            )

            Text(
                text = "Son kontrol: şimdi",
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 12.sp,
                color = AdminSecondaryText
            )
        }
    }
}