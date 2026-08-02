package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private val ScreenBackground = Color(0xFFF1F7F9)
private val DarkText = Color(0xFF14263D)
private val SecondaryText = Color(0xFF7890A2)
private val MainTeal = Color(0xFF1D6679)
private val SoftGreen = Color(0xFFD4F3DF)

@Composable
fun HomeScreen(
    adimSayisi: Long? = null,
    ortalamaNabiz: Long? = null,
    uykuSuresi: String? = null,
    tahminiUyku: String? = null,
    onOpenCrisis: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    Scaffold(
        containerColor = ScreenBackground,
        bottomBar = {
            DashboardBottomBar(
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
                DashboardHeader()
            }

            item {
                DailyStatusCard()
            }

            item {
                HealthDataCards()
            }

            item {
                RecoveryJourneyCard()
            }

            item {
                SupportRequestButton()
            }

            item {
                WaterTrackerCard()
            }

            item {
                QuickActions()
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Column {
        Text(
            text = "İyi günler",
            fontSize = 20.sp,
            color = SecondaryText
        )

        Text(
            text = "Büşra Su",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

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
private fun DailyStatusCard() {
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
                text = "Ruh hali: Sakin   •   Uyku: 7,2 saat   •   İstek: Düşük",
                modifier = Modifier.padding(top = 20.dp),
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}
@Composable
private fun HealthDataCards() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HealthCard(
            modifier = Modifier.weight(1f),
            title = "NABIZ",
            value = "72",
            unit = "bpm",
            description = "Dinlenme · Normal",
            symbol = "♥",
            backgroundColor = Color(0xFFFFE2DA),
            accentColor = Color(0xFFDE725B)
        )

        HealthCard(
            modifier = Modifier.weight(1f),
            title = "ADIM",
            value = "6.241",
            unit = "adım",
            description = "Günlük hedefin %83'ü",
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

            LinearProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(8.dp),
                color = Color(0xFF3C9FC0),
                trackColor = Color(0xFFD9EBEF)
            )

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
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    text = "47. Gün",
                    fontSize = 13.sp,
                    color = Color(0xFF2FA866)
                )
            }

            LinearProgressIndicator(
                progress = { 0.52f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(8.dp),
                color = Color(0xFF35AD6D),
                trackColor = Color(0xFFD9EBEF)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(20) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(
                                color = if (index < 11) {
                                    Color(0xFF35AD6D)
                                } else {
                                    Color(0xFFD9EBEF)
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportRequestButton() {
    Button(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD96849),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Text(
            text = "ⓘ",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Desteğe İhtiyacım Var",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
private fun QuickActions() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        QuickActionCard(
            modifier = Modifier.weight(1f),
            emoji = "🧘",
            title = "Nefes Egzersizi"
        )

        QuickActionCard(
            modifier = Modifier.weight(1f),
            emoji = "📞",
            title = "Yakınını Ara"
        )
    }
}
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {

        Column(
            modifier = Modifier
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = emoji,
                fontSize = 36.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )

        }

    }
}
@Composable
private fun DashboardBottomBar(
    onNavigate: (String) -> Unit
) {
    var selectedItem by remember {
        mutableIntStateOf(0)
    }

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
        containerColor = Color.White
    ) {

        var selectedItem by remember {
            mutableIntStateOf(0)
        }

        navigationItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    onNavigate(item.route)
                },
                icon = {
                    Text(
                        text = item.symbol,
                        fontSize = 22.sp
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 10.sp
                    )
                }
            )
        }
    }
}
private data class BottomNavigationItem(
    val symbol: String,
    val title: String,
    val route: String
)

