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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
@Composable
fun RegisterScreen(
    registrationError: String? = null,
    onRegister: (
        fullName: String,
        email: String,
        password: String,
        role: UserRole
    ) -> Unit,
    onBack: () -> Unit
) {

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var selectedRole by remember {
        mutableStateOf(UserRole.PATIENT)
    }

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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Hesap türü",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleSelectionButton(
                        modifier = Modifier.weight(1f),
                        title = "Hasta",
                        selected = selectedRole == UserRole.PATIENT,
                        onClick = {
                            selectedRole = UserRole.PATIENT
                        }
                    )

                    RoleSelectionButton(
                        modifier = Modifier.weight(1f),
                        title = "Doktor",
                        selected = selectedRole == UserRole.DOCTOR,
                        onClick = {
                            selectedRole = UserRole.DOCTOR
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleSelectionButton(
                        modifier = Modifier.weight(1f),
                        title = "Hasta Yakını",
                        selected = selectedRole == UserRole.RELATIVE,
                        onClick = {
                            selectedRole = UserRole.RELATIVE
                        }
                    )

                    RoleSelectionButton(
                        modifier = Modifier.weight(1f),
                        title = "Admin",
                        selected = selectedRole == UserRole.ADMIN,
                        onClick = {
                            selectedRole = UserRole.ADMIN
                        }
                    )
                }
            }

            val visibleError = error.ifBlank {
                registrationError.orEmpty()
            }

            if (visibleError.isNotEmpty()) {
                Text(
                    text = visibleError,
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
                                email.trim(),
                                password,
                                selectedRole
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

@Composable
private fun RoleSelectionButton(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                Color(0xFFD8F0F2)
            } else {
                Color.White
            },
            contentColor = Color(0xFF1D6679)
        )
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}