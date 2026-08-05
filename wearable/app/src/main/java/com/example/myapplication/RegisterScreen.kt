package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction

private val RegisterPrimary = Color(0xFF1D6679)
private val RegisterSecondaryText = Color(0xFF5F7F80)
private val RegisterFieldBackground = Color(0xFFFDFEF9)

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
    var fullName by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var selectedRole by remember {
        mutableStateOf(UserRole.PATIENT)
    }

    var localError by remember {
        mutableStateOf<String?>(null)
    }

    val visibleError = localError ?: registrationError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFCDEAF1),
                        Color(0xFFEAF7F9),
                        Color(0xFFF5ECD4)
                    )
                )
            )
    ) {
        RegisterNetworkBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 28.dp,
                    vertical = 28.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            Text(
                text = "Kayıt Ol",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = RegisterPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            RegisterTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    localError = null
                },
                placeholder = "Ad Soyad",
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )

            Spacer(modifier = Modifier.height(13.dp))

            RegisterTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                placeholder = "E-posta",
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(13.dp))

            RegisterTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                placeholder = "Şifre",
                keyboardType = KeyboardType.Password,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(13.dp))

            RegisterTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localError = null
                },
                placeholder = "Şifre Tekrar",
                keyboardType = KeyboardType.Password,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hesap türü",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = RegisterPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                RegisterRoleButton(
                    modifier = Modifier.weight(1f),
                    title = "Hasta",
                    selected = selectedRole == UserRole.PATIENT,
                    onClick = {
                        selectedRole = UserRole.PATIENT
                        localError = null
                    }
                )

                RegisterRoleButton(
                    modifier = Modifier.weight(1f),
                    title = "Doktor",
                    selected = selectedRole == UserRole.DOCTOR,
                    onClick = {
                        selectedRole = UserRole.DOCTOR
                        localError = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                RegisterRoleButton(
                    modifier = Modifier.weight(1f),
                    title = "Hasta Yakını",
                    selected = selectedRole == UserRole.RELATIVE,
                    onClick = {
                        selectedRole = UserRole.RELATIVE
                        localError = null
                    }
                )

                RegisterRoleButton(
                    modifier = Modifier.weight(1f),
                    title = "Admin",
                    selected = selectedRole == UserRole.ADMIN,
                    onClick = {
                        selectedRole = UserRole.ADMIN
                        localError = null
                    }
                )
            }

            if (!visibleError.isNullOrBlank()) {
                Text(
                    text = visibleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFC45145)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    when {
                        fullName.isBlank() -> {
                            localError =
                                "Lütfen ad soyad bilgini gir."
                        }

                        email.isBlank() -> {
                            localError =
                                "Lütfen e-posta adresini gir."
                        }

                        !email.contains("@") -> {
                            localError =
                                "Lütfen geçerli bir e-posta adresi gir."
                        }

                        password.isBlank() -> {
                            localError =
                                "Lütfen şifreni gir."
                        }

                        password.length < 6 -> {
                            localError =
                                "Şifre en az 6 karakter olmalıdır."
                        }

                        password != confirmPassword -> {
                            localError =
                                "Şifreler birbiriyle eşleşmiyor."
                        }

                        else -> {
                            onRegister(
                                fullName.trim(),
                                email.trim(),
                                password,
                                selectedRole
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegisterPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 7.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "Hesap Oluştur",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "Giriş ekranına dön",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clickable {
                        onBack()
                    },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = RegisterPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    capitalization: KeyboardCapitalization =
        KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 16.sp,
                color = RegisterSecondaryText
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = ImeAction.Next
        ),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RegisterPrimary,
            unfocusedBorderColor = Color(0xFFB8D5C8),
            focusedContainerColor =
                RegisterFieldBackground.copy(alpha = 0.92f),
            unfocusedContainerColor =
                RegisterFieldBackground.copy(alpha = 0.86f),
            cursorColor = RegisterPrimary
        )
    )
}

@Composable
private fun RegisterRoleButton(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            RegisterPrimary
        } else {
            RegisterFieldBackground.copy(alpha = 0.86f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) {
                RegisterPrimary
            } else {
                Color(0xFFB8D5C8)
            }
        ),
        shadowElevation = if (selected) {
            4.dp
        } else {
            0.dp
        }
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    Color.White
                } else {
                    RegisterPrimary
                }
            )
        }
    }
}

@Composable
private fun RegisterNetworkBackdrop() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val nodes = listOf(
            Offset(size.width * 0.20f, size.height * 0.04f),
            Offset(size.width * 0.68f, size.height * 0.12f),
            Offset(size.width * 0.90f, size.height * 0.30f),
            Offset(size.width * 0.48f, size.height * 0.34f),
            Offset(size.width * 0.08f, size.height * 0.44f),
            Offset(size.width * 0.82f, size.height * 0.58f),
            Offset(size.width * 0.30f, size.height * 0.66f),
            Offset(size.width * 0.58f, size.height * 0.74f),
            Offset(size.width * 0.92f, size.height * 0.82f),
            Offset(size.width * 0.16f, size.height * 0.92f),
            Offset(size.width * 0.46f, size.height * 0.98f)
        )

        val links = listOf(
            0 to 1,
            1 to 2,
            1 to 3,
            3 to 4,
            3 to 5,
            2 to 5,
            4 to 6,
            3 to 6,
            6 to 7,
            5 to 7,
            7 to 8,
            6 to 9,
            9 to 10,
            7 to 10
        )

        links.forEach { (start, end) ->
            drawLine(
                color = Color(0xFF1D6679).copy(alpha = 0.13f),
                start = nodes[start],
                end = nodes[end],
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.cornerPathEffect(
                    3.dp.toPx()
                )
            )
        }

        nodes.forEachIndexed { index, point ->
            drawCircle(
                color = RegisterPrimary.copy(
                    alpha = if (index % 3 == 0) {
                        0.18f
                    } else {
                        0.12f
                    }
                ),
                radius = if (index % 3 == 0) {
                    6.dp.toPx()
                } else {
                    4.dp.toPx()
                },
                center = point,
                style = Stroke(
                    width = 1.5.dp.toPx()
                )
            )

            drawCircle(
                color = RegisterPrimary.copy(alpha = 0.10f),
                radius = 2.5.dp.toPx(),
                center = point
            )
        }
    }
}