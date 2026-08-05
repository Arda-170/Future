package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoleProfileScreen(
    userName: String,
    userEmail: String,
    userRole: UserRole,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val background = Color(0xFFF1F7F9)
    val darkText = Color(0xFF14263D)
    val secondaryText = Color(0xFF7890A2)
    val mainTeal = Color(0xFF1D6679)

    val displayName = userName
        .trim()
        .ifBlank {
            userEmail
                .substringBefore("@")
                .replaceFirstChar { character ->
                    character.uppercase()
                }
        }

    val initial = displayName
        .firstOrNull()
        ?.uppercase()
        ?: "K"

    val roleTitle = when (userRole) {
        UserRole.PATIENT -> "Hasta"
        UserRole.DOCTOR -> "Doktor"
        UserRole.RELATIVE -> "Hasta Yakını"
        UserRole.ADMIN -> "Admin"
    }

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
                    bottom = 36.dp
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(94.dp),
                    shape = CircleShape,
                    color = mainTeal
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 39.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = displayName,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkText
                )

                Text(
                    text = userEmail,
                    modifier = Modifier.padding(
                        top = 4.dp
                    ),
                    fontSize = 13.sp,
                    color = secondaryText
                )

                Surface(
                    modifier = Modifier.padding(
                        top = 10.dp
                    ),
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFD8F0F2)
                ) {
                    Text(
                        text = roleTitle,
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color = mainTeal
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            RoleProfileOptionCard(
                title = "Hesap Bilgileri",
                description =
                    "Ad, e-posta ve hesap türünü görüntüle."
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            RoleProfileOptionCard(
                title = "Gizlilik ve İzinler",
                description =
                    "Veri erişimi ve gizlilik tercihlerini yönet."
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            RoleProfileOptionCard(
                title = "Ayarlar",
                description =
                    "Tercihlerini düzenle."
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFFD96849),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Çıkış Yap",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

@Composable
private fun RoleProfileOptionCard(
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
            verticalAlignment =
                Alignment.CenterVertically
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
                    modifier = Modifier.padding(
                        top = 5.dp
                    ),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF7890A2)
                )
            }

            Text(
                text = "›",
                fontSize = 27.sp,
                color = Color(0xFF7890A2)
            )
        }
    }
}