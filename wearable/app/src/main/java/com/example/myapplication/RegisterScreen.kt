package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(
    onRegister: (
        fullName: String,
        email: String
    ) -> Unit,
    onBack: () -> Unit
) {

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var error by remember { mutableStateOf("") }

    val mainColor = Color(0xFF1D6679)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF1F7F9)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Hesap Oluştur",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Ad Soyad") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Şifre") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Şifre Tekrar") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = Color.Red
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mainColor
                ),
                onClick = {

                    when {

                        fullName.isBlank() ->
                            error = "Lütfen ad soyad giriniz."

                        email.isBlank() ->
                            error = "Lütfen e-posta giriniz."

                        password.length < 6 ->
                            error = "Şifre en az 6 karakter olmalıdır."

                        password != confirmPassword ->
                            error = "Şifreler eşleşmiyor."

                        else -> {
                            onRegister(
                                fullName.trim(),
                                email.trim()
                            )
                        }
                    }

                }

            ) {

                Text("Hesap Oluştur")

            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onBack
            ) {
                Text("Giriş ekranına dön")
            }

        }

    }

}