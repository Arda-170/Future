package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onStartBreathing: () -> Unit = {},
    onCallRelative: () -> Unit = {},
    onCallAuthority: () -> Unit = {}
) {

    val Background = Color(0xFFF1F7F9)
    val MainBlue = Color(0xFF1D6679)
    val DarkText = Color(0xFF14263D)
    val SecondaryText = Color(0xFF7890A2)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("← Geri")
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Sakin Kal, Güvendesin",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Kendini zorlanmış hissediyorsan aşağıdaki seçeneklerden sana en uygun olanı kullanabilirsin.",
                fontSize = 16.sp,
                color = SecondaryText,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Nefes Egzersizi
            OutlinedButton(
                onClick = onStartBreathing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "🧘  Nefes Egzersizi",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MainBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Yakınımı Ara
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
                    color = MainBlue
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Destek
            Button(
                onClick = onCallAuthority,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD96849)
                )
            ) {
                Text(
                    text = "🏥 Destek Ekibini Ara",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Unutma, yardım istemek güçsüzlük değil, iyileşmenin bir parçasıdır.",
                color = SecondaryText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bu ekran acil tıbbi müdahalenin yerine geçmez.",
                color = SecondaryText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}