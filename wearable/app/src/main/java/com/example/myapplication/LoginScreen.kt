package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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

private val LoginPrimary = Color(0xFF1D6679)
private val LoginSecondaryText = Color(0xFF7890A2)
private val LoginAccent = Color(0xFF4F7F8D)
private val LoginFieldBackground = Color(0xFFFDFEFE)
private val LoginFieldBorder = Color(0xFFB8D5DE)
private val LoginErrorColor = Color(0xFFC45145)

@Composable
fun LoginScreen(
    loginError: String? = null,
    onLogin: (
        email: String,
        password: String
    ) -> Unit,
    onRegister: () -> Unit = {},
    onForgotPassword: () -> Unit = {}
) {
    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var localError by remember {
        mutableStateOf<String?>(null)
    }

    val visibleError = localError ?: loginError

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
        LoginNetworkBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.9f))

            Text(
                text = "FUTURE",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = LoginPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Küçük adımlar, büyük zaferler",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = LoginAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            LoginTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                placeholder = "E-posta",
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(14.dp))

            LoginTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                placeholder = "Şifre",
                keyboardType = KeyboardType.Password,
                isPassword = true
            )

            Text(
                text = "Şifreni mi unuttun?",
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 14.dp)
                    .clickable {
                        onForgotPassword()
                    },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = LoginSecondaryText
            )

            if (!visibleError.isNullOrBlank()) {
                Text(
                    text = visibleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    fontSize = 13.sp,
                    color = LoginErrorColor
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    when {
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
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginPrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 7.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "Giriş Yap",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.padding(top = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hesabın yok mu? ",
                    fontSize = 15.sp,
                    color = LoginSecondaryText
                )

                Text(
                    text = "Kayıt Ol",
                    modifier = Modifier.clickable {
                        onRegister()
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoginPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1.1f))
        }
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 16.sp,
                color = LoginSecondaryText
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LoginPrimary,
            unfocusedBorderColor = LoginFieldBorder,
            focusedContainerColor =
                LoginFieldBackground.copy(alpha = 0.94f),
            unfocusedContainerColor =
                LoginFieldBackground.copy(alpha = 0.88f),
            cursorColor = LoginPrimary,
            focusedTextColor = Color(0xFF14263D),
            unfocusedTextColor = Color(0xFF14263D)
        )
    )
}

@Composable
private fun LoginNetworkBackdrop() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val networkColor = Color(0xFF1D6679)

        val nodes = listOf(
            Offset(size.width * 0.03f, size.height * 0.10f),
            Offset(size.width * 0.25f, size.height * 0.01f),
            Offset(size.width * 0.71f, size.height * 0.15f),
            Offset(size.width * 0.98f, size.height * 0.07f),
            Offset(size.width * 0.04f, size.height * 0.33f),
            Offset(size.width * 0.45f, size.height * 0.43f),
            Offset(size.width * 0.86f, size.height * 0.59f),
            Offset(size.width * 0.18f, size.height * 0.95f)
        )

        val links = listOf(
            0 to 1,
            1 to 2,
            2 to 3,
            0 to 4,
            1 to 4,
            4 to 5,
            2 to 5,
            5 to 6,
            4 to 7,
            5 to 7,
            3 to 6
        )

        links.forEach { (start, end) ->
            drawLine(
                color = networkColor.copy(alpha = 0.13f),
                start = nodes[start],
                end = nodes[end],
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.cornerPathEffect(
                    3.dp.toPx()
                )
            )
        }

        nodes.forEachIndexed { index, point ->
            drawCircle(
                color = networkColor.copy(
                    alpha = if (index % 3 == 0) {
                        0.20f
                    } else {
                        0.14f
                    }
                ),
                radius = if (index % 3 == 0) {
                    7.dp.toPx()
                } else {
                    5.dp.toPx()
                },
                center = point,
                style = Stroke(
                    width = 1.5.dp.toPx()
                )
            )

            drawCircle(
                color = networkColor.copy(alpha = 0.12f),
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}