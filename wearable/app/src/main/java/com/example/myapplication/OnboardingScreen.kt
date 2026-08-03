package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
private data class OnboardingPage(
    val symbol: String,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit = {}
) {
    val pages = listOf(
        OnboardingPage(
            symbol = "♡",
            title = "Yanındayız",
            description = "Günlük sağlık verilerini takip ederek iyileşme sürecini daha anlaşılır hâle getir."
        ),
        OnboardingPage(
            symbol = "⌁",
            title = "Sağlık Verilerini Takip Et",
            description = "Adım, nabız, uyku ve egzersiz bilgilerini tek bir ekranda görüntüle."
        ),
        OnboardingPage(
            symbol = "✓",
            title = "Güvenli Destek",
            description = "Zorlandığın anlarda nefes egzersizine ulaşabilir ve güvendiğin kişileri arayabilirsin."
        )
    )

    var currentPage by remember {
        mutableIntStateOf(0)
    }

    val current = pages[currentPage]

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF1F7F9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (currentPage < pages.lastIndex) "Atla" else "",
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
                    .clickable(
                        enabled = currentPage < pages.lastIndex
                    ) {
                        onFinish()
                    }
                    .padding(
                        horizontal = 8.dp,
                        vertical = 6.dp
                    ),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1D6679)
            )

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color(0xFFD8F0F2)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = current.symbol,
                        fontSize = 56.sp,
                        color = Color(0xFF1D6679)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = current.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14263D),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = current.description,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Color(0xFF7890A2),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .width(if (index == currentPage) 26.dp else 8.dp)
                            .height(8.dp)
                            .background(
                                color = if (index == currentPage) {
                                    Color(0xFF1D6679)
                                } else {
                                    Color(0xFFD2E3E7)
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (currentPage < pages.lastIndex) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1D6679)
                )
            ) {
                Text(
                    text = if (currentPage < pages.lastIndex) {
                        "Devam Et"
                    } else {
                        "Başlayalım"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
