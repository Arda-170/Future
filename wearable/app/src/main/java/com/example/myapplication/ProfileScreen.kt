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

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {}
) {
    val background = Color(0xFFF1F7F9)
    val darkText = Color(0xFF14263D)
    val secondaryText = Color(0xFF7890A2)
    val mainTeal = Color(0xFF1D6679)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
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

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(94.dp),
                    shape = CircleShape,
                    color = mainTeal
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            fontSize = 39.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Büşra Çamalan",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkText
                )

                Text(
                    text = "47 günlük yolculuk",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 14.sp,
                    color = secondaryText
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    value = "1.250",
                    title = "Toplam Puan"
                )

                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    value = "18",
                    title = "Görev"
                )

                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    value = "21",
                    title = "En Uzun Seri"
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            ProfileOptionCard(
                title = "Kişisel Bilgiler",
                description = "Profil ve iletişim bilgilerini görüntüle."
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionCard(
                title = "Yakınlarım",
                description = "Kriz anında aranabilecek kişileri düzenle."
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionCard(
                title = "Gizlilik ve İzinler",
                description = "KVKK tercihlerini ve sağlık izinlerini yönet."
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileOptionCard(
                title = "Bildirim Ayarları",
                description = "Görev ve sağlık hatırlatmalarını düzenle."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileStatCard(
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
                vertical = 16.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D6679)
            )

            Text(
                text = title,
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 11.sp,
                color = Color(0xFF7890A2)
            )
        }
    }
}

@Composable
private fun ProfileOptionCard(
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
                    color = Color(0xFF14263D)
                )

                Text(
                    text = description,
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF7890A2)
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = Color(0xFF7890A2)
            )
        }
    }
}

