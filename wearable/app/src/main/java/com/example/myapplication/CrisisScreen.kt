package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrisisScreen(
    onBack: () -> Unit = {},
    onCallRelative: () -> Unit = {},
    onCallAuthority: () -> Unit = {}
) {
    val background = Color(0xFFF1F7F9)
    val mainBlue = Color(0xFF1D6679)
    val darkText = Color(0xFF14263D)
    val secondaryText = Color(0xFF7890A2)
    val supportColor = Color(0xFFD96849)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.align(
                    Alignment.Start
                )
            ) {
                Text("← Geri")
            }

            Spacer(
                modifier = Modifier.height(42.dp)
            )

            Text(
                text = "Sakin Kal, Güvendesin",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = darkText,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Kendini zorlanmış hissediyorsan aşağıdaki seçeneklerden sana en uygun olanı kullanabilirsin.",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            OutlinedButton(
                onClick = onCallRelative,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "📞 Yakınımı Ara",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = mainBlue
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onCallAuthority,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = supportColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "🏥 Destek Ekibini Ara",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(80.dp)
            )

            Text(
                text = "Unutma, yardım istemek güçsüzlük değil, iyileşmenin bir parçasıdır.",
                modifier = Modifier.fillMaxWidth(),
                color = secondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "Bu ekran acil tıbbi müdahalenin yerine geçmez.",
                modifier = Modifier.fillMaxWidth(),
                color = secondaryText,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}