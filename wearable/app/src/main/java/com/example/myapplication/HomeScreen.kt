package com.example.myapplication

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBackground = Color(0xFFF1F7F9)
private val DarkText = Color(0xFF14263D)
private val SecondaryText = Color(0xFF7890A2)
private val MainTeal = Color(0xFF1D6679)
private val SoftGreen = Color(0xFFD4F3DF)

@Composable
fun HomeScreen(
    userName: String,
    adimSayisi: Long? = null,
    ortalamaNabiz: Long? = null,
    sonNabiz: Long? = null,
    uykuSuresi: String? = null,
    tahminiUyku: String? = null,
    onOpenCrisis: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    val displayName = userName
        .trim()
        .substringBefore(" ")
        .ifBlank { "Kullanıcı" }

    Scaffold(
        containerColor = ScreenBackground,
        bottomBar = {
            DashboardBottomBar(
                currentRoute = "home",
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardHeader(
                    displayName = displayName,
                    onOpenProfile = onOpenProfile,
                    onOpenNotifications = onOpenNotifications
                )
            }

            item {
                DailyStatusCard(
                    sleepDuration = if (
                        uykuSuresi.isNullOrBlank() ||
                        uykuSuresi == "Veri yok"
                    ) {
                        tahminiUyku
                    } else {
                        uykuSuresi
                    }
                )
            }

            item {
                HealthDataCards(
                    heartRate = sonNabiz ?: ortalamaNabiz,
                    stepCount = adimSayisi
                )
            }

            item {
                WeeklyStreakCard()
            }

            item {
                RecoveryJourneyCard()
            }

            item {
                WaterTrackerCard()
            }

            item {
                SupportRequestButton(
                    onClick = onOpenCrisis
                )
            }

            item {
                QuickActions()
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    displayName: String,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "İyi günler!",
                    fontSize = 20.sp,
                    color = SecondaryText
                )

                Text(
                    text = displayName,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable {
                            onOpenNotifications()
                        },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔔",
                            fontSize = 21.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable {
                            onOpenProfile()
                        },
                    shape = CircleShape,
                    color = MainTeal,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName
                                .firstOrNull()
                                ?.uppercase()
                                ?: "K",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.padding(top = 10.dp),
            shape = RoundedCornerShape(50),
            color = SoftGreen
        ) {
            Text(
                text = "● Stabil   Son kontrol: 2 saat önce",
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
                fontSize = 12.sp,
                color = Color(0xFF218552)
            )
        }
    }
}

@Composable
private fun DailyStatusCard(
    sleepDuration: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MainTeal
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "BUGÜNÜN DURUMU",
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = Color(0xFFD5EBEF)
            )

            Text(
                text = "47",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "temiz gün",
                fontSize = 14.sp,
                color = Color(0xFFD5EBEF)
            )

            Text(
                text = "Ruh hali: Sakin  •  Uyku: ${
                    sleepDuration ?: "Veri yok"
                }  •  İstek: Düşük",
                modifier = Modifier.padding(top = 20.dp),
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HealthDataCards(
    heartRate: Long?,
    stepCount: Long?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HealthCard(
            modifier = Modifier.weight(1f),
            title = "NABIZ",
            value = heartRate?.toString() ?: "--",
            unit = "bpm",
            description = if (heartRate == null) {
                "Veri bulunamadı"
            } else {
                "Son ölçülen değer"
            },
            symbol = "♥",
            backgroundColor = Color(0xFFFFE2DA),
            accentColor = Color(0xFFDE725B)
        )

        HealthCard(
            modifier = Modifier.weight(1f),
            title = "ADIM",
            value = stepCount
                ?.let { value ->
                    formatNumber(value)
                }
                ?: "--",
            unit = "adım",
            description = if (stepCount == null) {
                "Veri bulunamadı"
            } else {
                "Bugünkü toplam"
            },
            symbol = "⌁",
            backgroundColor = Color(0xFFD6EFF6),
            accentColor = Color(0xFF287B98)
        )
    }
}

@Composable
private fun HealthCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    description: String,
    symbol: String,
    backgroundColor: Color,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = accentColor
                )

                Text(
                    text = symbol,
                    fontSize = 20.sp,
                    color = accentColor
                )
            }

            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Text(
                    text = unit,
                    modifier = Modifier.padding(
                        start = 3.dp,
                        bottom = 5.dp
                    ),
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }

            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 11.sp,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun WeeklyStreakCard() {
    val completedDays = 5
    val days = listOf(
        "P",
        "S",
        "Ç",
        "P",
        "C",
        "C",
        "P"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "🔥 Haftalık Seri",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Text(
                        text = "Hedefine ulaşmana 2 gün kaldı.",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        color = SecondaryText
                    )
                }

                Text(
                    text = "$completedDays / 7",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD96849)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape,
                            color = if (index < completedDays) {
                                Color(0xFFD96849)
                            } else {
                                Color(0xFFE2EAEC)
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (
                                        index < completedDays
                                    ) {
                                        "✓"
                                    } else {
                                        day
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (
                                        index < completedDays
                                    ) {
                                        Color.White
                                    } else {
                                        SecondaryText
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = SecondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryJourneyCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "İyileşme Yolculuğu",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Text(
                    text = "🌱 47. Gün",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2FA866)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD9EBEF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.52f)
                        .background(Color(0xFF35AD6D))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Başlangıç",
                    fontSize = 11.sp,
                    color = SecondaryText
                )

                Text(
                    text = "%52 / 90 günlük hedef",
                    fontSize = 11.sp,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
private fun WaterTrackerCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Su Takibi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Text(
                        text = "6 / 8 bardak",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        color = SecondaryText
                    )
                }

                Text(
                    text = "💧",
                    fontSize = 26.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD9EBEF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.75f)
                        .background(Color(0xFF3C9FC0))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(8) { index ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (index < 6) {
                            Color(0xFFBDE7F2)
                        } else {
                            Color(0xFFE8F1F4)
                        }
                    ) {
                        Text(
                            text = "💧",
                            modifier = Modifier.padding(
                                vertical = 8.dp
                            ),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportRequestButton(
    onClick: () -> Unit
) {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "supportButtonAnimation"
        )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "supportButtonScale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD96849)
        )
    ) {
        Text(
            text = "!",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Desteğe İhtiyacım Var",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun QuickActions(
    onStartBreathing: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onStartBreathing()
            },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 22.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(62.dp),
                shape = CircleShape,
                color = Color(0xFFD8F0F2)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧘",
                        fontSize = 32.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Nefes Egzersizi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Text(
                    text =
                        "Kısa bir egzersizle nefesini yavaşlat ve sakinleş.",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = SecondaryText
                )
            }

            Text(
                text = "›",
                fontSize = 30.sp,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun DashboardBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val navigationItems = listOf(
        BottomNavigationItem(
            symbol = "⌂",
            title = "Ana Sayfa",
            route = "home"
        ),
        BottomNavigationItem(
            symbol = "▤",
            title = "Rapor",
            route = "report"
        ),
        BottomNavigationItem(
            symbol = "✓",
            title = "Görevler",
            route = "tasks"
        ),
        BottomNavigationItem(
            symbol = "🛒",
            title = "Market",
            route = "market"
        )
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 4.dp
    ) {
        navigationItems.forEach { item ->
            val isSelected =
                currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onNavigate(item.route)
                    }
                },
                icon = {
                    Text(
                        text = item.symbol,
                        fontSize = 22.sp,
                        fontWeight = if (isSelected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor =
                            Color(0xFF14263D),
                        selectedTextColor =
                            Color(0xFF14263D),
                        indicatorColor =
                            Color(0xFFE9DCF8),
                        unselectedIconColor =
                            Color(0xFF5F5965),
                        unselectedTextColor =
                            Color(0xFF5F5965)
                    )
            )
        }
    }
}

private data class BottomNavigationItem(
    val symbol: String,
    val title: String,
    val route: String
)

private fun formatNumber(
    value: Long
): String {
    return "%,d"
        .format(value)
        .replace(',', '.')
}