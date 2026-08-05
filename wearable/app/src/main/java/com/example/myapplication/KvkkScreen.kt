package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
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

@Composable
fun KvkkScreen(
    onBack: () -> Unit = {},
    onAccept: () -> Unit = {}
) {
    var kvkkAccepted by remember {
        mutableStateOf(false)
    }

    var healthDataAccepted by remember {
        mutableStateOf(false)
    }

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
                .windowInsetsPadding(
                    WindowInsets.statusBars
                )
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 32.dp
                )
        ) {
            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Geri")
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Gizlilik ve Açık Rıza",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = darkText
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text =
                    "Uygulamayı kullanmadan önce kişisel ve sağlık verilerinizin nasıl işlendiğini okuyup onaylamanız gerekir.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = secondaryText
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Aydınlatma Metni",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkText
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = """
Bu uygulama; adım sayısı, kalp atış hızı, uyku süresi ve egzersiz bilgileri gibi sağlık verilerini yalnızca kullanıcıya günlük özet sunmak amacıyla işler.

Veriler, kullanıcının açık rızası olmadan üçüncü kişilerle paylaşılmaz. Kullanıcı istediği zaman verdiği izni geri çekebilir ve verilerinin silinmesini talep edebilir.

Sağlık verileri, yürürlükteki kişisel verilerin korunması mevzuatına uygun şekilde işlenir.
                        """.trimIndent(),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = secondaryText
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            ConsentOption(
                checked = kvkkAccepted,
                onCheckedChange = {
                    kvkkAccepted = it
                },
                text =
                    "KVKK aydınlatma metnini okudum ve anladım."
            )

            ConsentOption(
                checked = healthDataAccepted,
                onCheckedChange = {
                    healthDataAccepted = it
                },
                text =
                    "Sağlık verilerimin uygulama tarafından işlenmesine açık rıza veriyorum."
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = onAccept,
                enabled =
                    kvkkAccepted &&
                            healthDataAccepted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mainTeal,
                    contentColor = Color.White,
                    disabledContainerColor =
                        Color(0xFFB9C9CD),
                    disabledContentColor =
                        Color.White
                )
            ) {
                Text(
                    text = "Onayla ve Devam Et",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text =
                    "Onay verdikten sonra Health Connect izin ekranına yönlendirileceksiniz.",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}

@Composable
private fun ConsentOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )

            Text(
                text = text,
                modifier = Modifier.padding(
                    start = 8.dp,
                    top = 12.dp,
                    end = 8.dp
                ),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF14263D)
            )
        }
    }

    Spacer(
        modifier = Modifier.height(10.dp)
    )
}