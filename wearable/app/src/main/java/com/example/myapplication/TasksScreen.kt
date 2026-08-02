package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TasksBackground = Color(0xFFF1F7F9)
private val TasksDarkText = Color(0xFF14263D)
private val TasksSecondaryText = Color(0xFF7890A2)
private val TasksTeal = Color(0xFF1D6679)
private val TasksGreen = Color(0xFF35AD6D)
private val TasksOrange = Color(0xFFD96849)

private data class TaskItem(
    val symbol: String,
    val title: String,
    val description: String,
    val points: Int,
    val progress: Float,
    val progressText: String,
    val completed: Boolean
)

@Composable
fun TasksScreen(
    onBack: () -> Unit = {}
) {
    val tasks = listOf(
        TaskItem(
            symbol = "✓",
            title = "Günlük Kontrol",
            description = "Bugünkü ruh hâli ve iyileşme kontrolünü tamamla.",
            points = 5,
            progress = 1f,
            progressText = "Tamamlandı",
            completed = true
        ),
        TaskItem(
            symbol = "👣",
            title = "Adım Hedefi",
            description = "Bugün 8.000 adım hedefine ulaş.",
            points = 10,
            progress = 0.78f,
            progressText = "6.241 / 8.000 adım",
            completed = false
        ),
        TaskItem(
            symbol = "💧",
            title = "Su Hedefi",
            description = "Gün içinde 8 bardak su iç.",
            points = 10,
            progress = 0.75f,
            progressText = "6 / 8 bardak",
            completed = false
        ),
        TaskItem(
            symbol = "🧘",
            title = "Nefes Egzersizi",
            description = "En az bir nefes egzersizi tamamla.",
            points = 15,
            progress = 0f,
            progressText = "Henüz başlamadı",
            completed = false
        ),
        TaskItem(
            symbol = "😴",
            title = "Uyku Hedefi",
            description = "En az 7 saat uyku kaydı oluştur.",
            points = 10,
            progress = 1f,
            progressText = "7 sa 12 dk",
            completed = true
        ),
        TaskItem(
            symbol = "🔥",
            title = "7 Günlük Seri",
            description = "Yedi gün boyunca günlük kontrolleri aksatma.",
            points = 50,
            progress = 0.71f,
            progressText = "5 / 7 gün",
            completed = false
        )
    )

    val earnedPoints = tasks
        .filter { it.completed }
        .sumOf { it.points }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TasksBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Geri")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Görevler",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TasksDarkText
            )

            Text(
                text = "Günlük hedeflerini tamamla ve puan kazan.",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 15.sp,
                color = TasksSecondaryText
            )

            Spacer(modifier = Modifier.height(22.dp))

            TaskPointsCard(
                earnedPoints = earnedPoints,
                completedTasks = tasks.count { it.completed },
                totalTasks = tasks.size
            )

            Spacer(modifier = Modifier.height(18.dp))

            tasks.forEach { task ->
                TaskCard(task = task)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TaskPointsCard(
    earnedPoints: Int,
    completedTasks: Int,
    totalTasks: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = TasksTeal
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(66.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.17f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 30.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "$earnedPoints puan kazandın",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "$completedTasks / $totalTasks görev tamamlandı",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFD5EBEF)
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskItem
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = if (task.completed) {
                        Color(0xFFDDF5E7)
                    } else {
                        Color(0xFFDCEFF2)
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.symbol,
                            fontSize = 23.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TasksDarkText
                    )

                    Text(
                        text = task.description,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TasksSecondaryText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFE7DE)
                ) {
                    Text(
                        text = "+${task.points}",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TasksOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (task.completed) {
                    TasksGreen
                } else {
                    TasksTeal
                },
                trackColor = Color(0xFFDCECEF)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.progressText,
                    fontSize = 12.sp,
                    color = TasksSecondaryText
                )

                Text(
                    text = if (task.completed) {
                        "Tamamlandı"
                    } else {
                        "%${(task.progress * 100).toInt()}"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.completed) {
                        TasksGreen
                    } else {
                        TasksTeal
                    }
                )
            }

            if (!task.completed && task.title == "Nefes Egzersizi") {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TasksTeal
                    )
                ) {
                    Text("Görevi Başlat")
                }
            }
        }
    }
}