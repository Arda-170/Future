package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Geçici Veri Modeli
data class RiskLocation(val id: String, val name: String, val latitude: Double, val longitude: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskLocationScreen(onBack: () -> Unit) {
    // Örnek kaydedilmiş lokasyonlar
    var savedLocations by remember { 
        mutableStateOf(listOf(
            RiskLocation("1", "Eski Mahalle (Kaçınılması Gereken)", 36.78, 31.43) // Manavgat koordinatlarına yakın bir örnek
        )) 
    }
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riskli Lokasyonlarım") },
                navigationIcon = {
                    Button(onClick = onBack, modifier = Modifier.padding(8.dp)) { Text("<") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1D6679)
            ) {
                Text("+", color = Color.White, fontSize = 24.sp)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "KVKK Aydınlatma: Eklediğiniz lokasyonlar yalnızca cihazınızda Geofence (Sanal Çit) oluşturmak için kullanılır. Arka planda sürekli GPS takibi yapılmaz. Seçtiğiniz alanlara girdiğinizde sistem sadece kriz tespiti için uyarılır.",
                fontSize = 12.sp,
                color = Color(0xFF7890A2),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(savedLocations) { location ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = location.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Durum: Takip Ediliyor (Geofence Aktif)", fontSize = 12.sp, color = Color(0xFF35AD6D))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Riskli Alan Ekle") },
            text = { Text("Mevcut konumunuz riskli alan olarak kaydedilecek ve izleme başlatılacaktır. Onaylıyor musunuz?") },
            confirmButton = {
                Button(onClick = { 
                    // Burada GeofenceManager çağrılıp bölge eklenecek
                    showAddDialog = false 
                }) { Text("Onaylıyorum") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("İptal") }
            }
        )
    }
}
