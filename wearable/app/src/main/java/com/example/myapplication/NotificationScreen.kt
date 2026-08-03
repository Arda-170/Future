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

private data class NotificationItem(
    val symbol: String,
    val title: String,
    val description: String,
    val time: String,
    val unread: Boolean
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {}
) {
    val notifications = listOf(
        NotificationItem(
            symbol = "✓",
            title = "Görev tamamlandı",
            description = "Uyku hedefini tamamladın ve 10 puan kazandın.",
            time = "10 dk önce",
            unread = true
        ),
        NotificationItem(
            symbol = "🔥",
            title = "Serin devam ediyor",
            description = "7 günlük hedefin için yalnızca 2 gün kaldı.",
            time = "1 saat önce",
            unread = true
        ),
        NotificationItem(
            symbol = "💧",
            title = "Su hatırlatıcısı",
            description = "Günlük su hedefinin 2 bardak gerisindesin.",
            time = "2 saat önce",
            unread = false
        ),
        NotificationItem(
            symbol = "🎁",
            title = "Yeni ödül",
            description = "Markette yeni bir sinema bileti ödülü açıldı.",
            time = "Dün",
            unread = false
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF1F7F9)
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

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Bildirimler",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14263D)
            )

            Text(
                text = "Görevlerin, sağlık hedeflerin ve ödüllerin.",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 14.sp,
                color = Color(0xFF7890A2)
            )

            Spacer(modifier = Modifier.height(20.dp))

            notifications.forEach { notification ->
                NotificationCard(notification)
                Spacer(modifier = Modifier.height(11.dp))
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (notification.unread) {
            Color(0xFFE4F5F6)
        } else {
            Color.White
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = Color(0xFFD8F0F2)
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

            Spacer(modifier = Modifier.width(13.dp))

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

            if (notification.unread) {
                Surface(
                    modifier = Modifier.size(9.dp),
                    shape = CircleShape,
                    color = Color(0xFFD96849)
                ) {}
            }
        }
    }
}

