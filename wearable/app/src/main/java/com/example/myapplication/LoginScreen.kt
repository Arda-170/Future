package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    loginError: String? = null,
    onLogin: (
        email: String,
        password: String
    ) -> Unit,
    onRegister: () -> Unit = {}
) {
    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = Color(0xFFD8F0F2)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♡",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Hoş geldin",
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = darkText,
                textAlign = TextAlign.Center
            )


            Spacer(modifier = Modifier.height(34.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("E-posta")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = mainTeal,
                    focusedLabelColor = mainTeal,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Şifre")
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = mainTeal,
                    focusedLabelColor = mainTeal,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            val visibleError = errorMessage ?: loginError

            if (visibleError != null) {
                Text(
                    text = visibleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFD96849)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    when {
                        email.isBlank() -> {
                            errorMessage = "Lütfen e-posta adresini gir."
                        }

                        password.isBlank() -> {
                            errorMessage = "Lütfen şifreni gir."
                        }

                        password.length < 6 -> {
                            errorMessage =
                                "Şifre en az 6 karakter olmalıdır."
                        }

                        else -> {
                            onLogin(
                                email.trim(),
                                password
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mainTeal
                )
            ) {
                Text(
                    text = "Giriş Yap",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onRegister,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Hesabın yok mu? Kayıt Ol",
                    fontSize = 14.sp,
                    color = mainTeal
                )
            }

            Spacer(modifier = Modifier.weight(1f))

        }
    }
}
