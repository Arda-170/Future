package com.example.myapplication

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.Scaffold
private val MarketBackground = Color(0xFFF1F7F9)
private val MarketDarkText = Color(0xFF14263D)
private val MarketSecondaryText = Color(0xFF7890A2)
private val MarketTeal = Color(0xFF1D6679)
private val MarketOrange = Color(0xFFD96849)

private data class MarketReward(
    val symbol: String,
    val title: String,
    val description: String,
    val category: String,
    val points: Int,
    val backgroundColor: Color
)

@Composable
fun MarketScreen(
    userPoints: Int,
    onRewardPurchased: (Int, String) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    var selectedCategory by remember {
        mutableStateOf("Tümü")
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val categories = listOf(
        "Tümü",
        "Yeme İçme",
        "Eğlence",
        "Alışveriş",
        "Sağlık",
        "Sürpriz"
    )

    val rewards = listOf(
        MarketReward(
            symbol = "☕",
            title = "Kahve Kuponu",
            description = "Anlaşmalı kafelerde geçerli bir orta boy kahve kuponu.",
            category = "Yeme İçme",
            points = 30,
            backgroundColor = Color(0xFFFFE8D8)
        ),
        MarketReward(
            symbol = "🥗",
            title = "Sağlıklı Menü",
            description = "Anlaşmalı restoranlarda geçerli sağlıklı menü indirimi.",
            category = "Yeme İçme",
            points = 650,
            backgroundColor = Color(0xFFDDF5E7)
        ),
        MarketReward(
            symbol = "🎬",
            title = "Sinema Bileti",
            description = "Anlaşmalı sinemalarda geçerli bir kişilik sinema bileti.",
            category = "Eğlence",
            points = 900,
            backgroundColor = Color(0xFFE1E4FF)
        ),
        MarketReward(
            symbol = "🎭",
            title = "Etkinlik Bileti",
            description = "Seçili tiyatro, konser veya kültür etkinliklerinde kullanılabilir.",
            category = "Eğlence",
            points = 1200,
            backgroundColor = Color(0xFFFFE4EF)
        ),
        MarketReward(
            symbol = "🛍️",
            title = "100 TL Alışveriş Çeki",
            description = "Anlaşmalı mağazalarda kullanılabilen alışveriş çeki.",
            category = "Alışveriş",
            points = 1500,
            backgroundColor = Color(0xFFDDF3FF)
        ),
        MarketReward(
            symbol = "📚",
            title = "Kitap İndirimi",
            description = "Anlaşmalı kitapçılarda geçerli indirim kuponu.",
            category = "Alışveriş",
            points = 700,
            backgroundColor = Color(0xFFFFF1C9)
        ),
        MarketReward(
            symbol = "🚌",
            title = "Ulaşım Desteği",
            description = "Toplu taşıma veya anlaşmalı ulaşım hizmetlerinde kullanılabilir.",
            category = "Sağlık",
            points = 800,
            backgroundColor = Color(0xFFDCEFF2)
        ),
        MarketReward(
            symbol = "🏊",
            title = "Spor Merkezi Girişi",
            description = "Anlaşmalı spor merkezinde bir günlük ücretsiz kullanım.",
            category = "Sağlık",
            points = 1100,
            backgroundColor = Color(0xFFDDF5E7)
        ),
        MarketReward(
            symbol = "🧘",
            title = "Yoga Dersi",
            description = "Anlaşmalı merkezde bir grup yoga veya meditasyon dersi.",
            category = "Sağlık",
            points = 1000,
            backgroundColor = Color(0xFFE6E1F7)
        ),
        MarketReward(
            symbol = "🎁",
            title = "Sürpriz Ödül Kutusu",
            description = "Rastgele bir kupon, bonus puan veya etkinlik ödülü kazandırır.",
            category = "Sürpriz",
            points = 750,
            backgroundColor = Color(0xFFFFE4EF)
        )
    )

    val visibleRewards = if (selectedCategory == "Tümü") {
        rewards
    } else {
        rewards.filter {
            it.category == selectedCategory
        }
    }

    Scaffold(
        containerColor = MarketBackground,
        bottomBar = {
            DashboardBottomBar(
                currentRoute = "market",
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 24.dp,
                    bottom = 20.dp
                )
        ) {
            Text(
                text = "Market",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MarketDarkText
            )

            Text(
                text = "Görevlerden kazandığın puanlarla ödülleri aç.",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 15.sp,
                color = MarketSecondaryText
            )

            Spacer(modifier = Modifier.height(22.dp))

            MarketPointsCard(
                userPoints = userPoints
            )

            if (message != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFDDF5E7)
                ) {
                    Text(
                        text = message.orEmpty(),
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        color = Color(0xFF24784A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    CategoryButton(
                        title = category,
                        selected =
                            selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            visibleRewards
                .chunked(2)
                .forEach { rowItems ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { reward ->
                            MarketRewardCard(
                                modifier =
                                    Modifier.weight(1f),
                                reward = reward,
                                userPoints = userPoints,
                                onBuy = {
                                    if (
                                        userPoints >=
                                        reward.points
                                    ) {
                                        onRewardPurchased(
                                            reward.points,
                                            reward.title
                                        )

                                        message =
                                            "${reward.title} başarıyla açıldı."
                                    } else {
                                        message =
                                            "Bu ödül için yeterli puanın bulunmuyor."
                                    }
                                }
                            )
                        }

                        if (rowItems.size == 1) {
                            Spacer(
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MarketPointsCard(
    userPoints: Int
) {
    val nextRewardTarget = 1500
    val progress = (
            userPoints.toFloat() / nextRewardTarget.toFloat()
            ).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MarketTeal
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🪙",
                            fontSize = 29.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(15.dp))

                Column {
                    Text(
                        text = "Puan Bakiyen",
                        fontSize = 13.sp,
                        color = Color(0xFFD5EBEF)
                    )

                    Text(
                        text = "$userPoints puan",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF48DECD),
                trackColor = Color.White.copy(alpha = 0.20f)
            )
        }
    }
}

@Composable
private fun CategoryButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MarketTeal
            } else {
                Color.White
            },
            contentColor = if (selected) {
                Color.White
            } else {
                MarketDarkText
            }
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 9.dp
        )
    ) {
        Text(
            text = title,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MarketRewardCard(
    modifier: Modifier = Modifier,
    reward: MarketReward,
    userPoints: Int,
    onBuy: () -> Unit
) {
    val canBuy = userPoints >= reward.points

    Surface(
        modifier = modifier.height(255.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(14.dp),
                color = reward.backgroundColor
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reward.symbol,
                        fontSize = 39.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = reward.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MarketDarkText,
                textAlign = TextAlign.Center
            )

            Text(
                text = reward.description,
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MarketSecondaryText,
                textAlign = TextAlign.Center,
                maxLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${reward.points} puan",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MarketOrange
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth(),
                enabled = canBuy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MarketTeal,
                    disabledContainerColor = Color(0xFFC4D0D3)
                ),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text(
                    text = if (canBuy) {
                        "Ödülü Aç"
                    } else {
                        "Puan Yetersiz"
                    },
                    fontSize = 12.sp
                )
            }
        }
    }
}