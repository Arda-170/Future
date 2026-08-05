package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
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

private data class RoleNotificationItem(
    val symbol: String,
    val title: String,
    val description: String,
    val time: String,
    val important: Boolean = false
)

@Composable
fun RoleNotificationsScreen(
    userRole: UserRole,
    onBack: () -> Unit = {}
) {
    val notifications = when (userRole) {
        UserRole.DOCTOR -> doctorNotifications()
        UserRole.RELATIVE -> relativeNotifications()
        UserRole.ADMIN -> adminNotifications()
        UserRole.PATIENT -> emptyList()
    }

    val screenDescription = when (userRole) {
        UserRole.DOCTOR ->
            "Takip ettiğiniz hastalara ait güncel bildirimler."

        UserRole.RELATIVE ->
            "Yakınlarınızın paylaşılan sağlık güncellemeleri."

        UserRole.ADMIN ->
            "Sistem ve kullanıcı işlemlerine ait bildirimler."

        UserRole.PATIENT ->
            "Güncel bildirimleriniz."
    }

    Scaffold(
        containerColor = Color(0xFFF1F7F9)
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = onBack
                ) {
                    Text("← Geri")
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 8.dp
                    )
                ) {
                    Text(
                        text = "Bildirimler",
                        fontSize = 28.sp,
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

            if (notifications.isEmpty()) {
                item {
                    EmptyRoleNotificationsCard()
                }
            } else {
                items(
                    items = notifications
                ) { notification ->
                    RoleNotificationCard(
                        notification = notification
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleNotificationCard(
    notification: RoleNotificationItem
) {
    val backgroundColor = if (notification.important) {
        Color(0xFFFFE8E1)
    } else {
        Color.White
    }

    val symbolBackground = if (notification.important) {
        Color(0xFFFFD8CE)
    } else {
        Color(0xFFD8F0F2)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = symbolBackground
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.symbol,
                        fontSize = 21.sp
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
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF14263D)
                )

                Text(
                    text = notification.description,
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF7890A2)
                )

                Text(
                    text = notification.time,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 11.sp,
                    color = Color(0xFF7890A2)
                )
            }

            if (notification.important) {
                Surface(
                    modifier = Modifier.size(9.dp),
                    shape = CircleShape,
                    color = Color(0xFFD96849)
                ) {}
            }
        }
    }
}

@Composable
private fun EmptyRoleNotificationsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔔",
                fontSize = 30.sp
            )

            Text(
                text = "Yeni bildiriminiz yok",
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14263D)
            )

            Text(
                text = "Yeni bir gelişme olduğunda burada gösterilecektir.",
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF7890A2)
            )
        }
    }
}

private fun doctorNotifications(): List<RoleNotificationItem> {
    return listOf(
        RoleNotificationItem(
            symbol = "!",
            title = "Ayşe Demir için risk uyarısı",
            description =
                "Son sağlık değerlendirmesinde takip gerektiren bir değişiklik belirlendi.",
            time = "Şimdi",
            important = true
        ),
        RoleNotificationItem(
            symbol = "😴",
            title = "Uyku hedefi tamamlanmadı",
            description =
                "Ahmet Yılmaz’ın son uyku süresi günlük hedefin altında kaldı.",
            time = "20 dk önce"
        ),
        RoleNotificationItem(
            symbol = "✓",
            title = "Sağlık verileri güncellendi",
            description =
                "Mehmet Çelik’in adım, nabız ve uyku verileri güncellendi.",
            time = "1 saat önce"
        )
    )
}

private fun relativeNotifications(): List<RoleNotificationItem> {
    return listOf(
        RoleNotificationItem(
            symbol = "✓",
            title = "Sağlık verileri güncellendi",
            description =
                "Büşra Çamalan’ın son sağlık verileri sisteme aktarıldı.",
            time = "15 dk önce"
        ),
        RoleNotificationItem(
            symbol = "😴",
            title = "Uyku kaydı oluşturuldu",
            description =
                "Yakınınızın son uyku kaydı görüntülenmeye hazır.",
            time = "2 saat önce"
        ),
        RoleNotificationItem(
            symbol = "👣",
            title = "Günlük aktivite bilgisi",
            description =
                "Yakınınız bugün adım hedefinin yüzde 75’ine ulaştı.",
            time = "Bugün"
        )
    )
}

private fun adminNotifications(): List<RoleNotificationItem> {
    return listOf(
        RoleNotificationItem(
            symbol = "!",
            title = "Health Connect bağlantısı kesildi",
            description =
                "Bir kullanıcının sağlık verisi bağlantısı yeniden izin bekliyor.",
            time = "5 dk önce",
            important = true
        ),
        RoleNotificationItem(
            symbol = "👤",
            title = "Yeni kullanıcı kaydı",
            description =
                "Sisteme yeni bir doktor hesabı kaydedildi.",
            time = "35 dk önce"
        ),
        RoleNotificationItem(
            symbol = "✓",
            title = "Sistem kontrolü tamamlandı",
            description =
                "Tüm temel servislerin çalıştığı doğrulandı.",
            time = "1 saat önce"
        )
    )
}