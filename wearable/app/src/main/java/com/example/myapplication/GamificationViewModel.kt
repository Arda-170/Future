package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GamificationState(
    val currentPoints: Int = 0,
    val currentLevel: Int = 1,
    val unlockedBadges: List<String> = emptyList()
)

class GamificationViewModel : ViewModel() {

    private val _state = MutableStateFlow(GamificationState())
    val state: StateFlow<GamificationState> = _state.asStateFlow()

    fun evaluateAction(actionType: String) {
        val currentState = _state.value
        var earnedPoints = 0

        when (actionType) {
            "GEOFENCE_AVOIDED_24H" -> earnedPoints = 50  //Riskli alana 1 gün gidilmedi
            "SCREEN_TIME_REDUCED" -> earnedPoints = 20   //Telefon ekran süresi azaltıldı
            "GOOD_SLEEP_RECORDED" -> earnedPoints = 15   //Health Connect'ten 7+ saat uyku geldi
        }

        val newTotal = currentState.currentPoints + earnedPoints
        val newLevel = calculateLevel(newTotal)
        val newBadges = checkBadges(newTotal, currentState.unlockedBadges)

        _state.value = currentState.copy(
            currentPoints = newTotal,
            currentLevel = newLevel,
            unlockedBadges = newBadges
        )
    }

    private fun calculateLevel(points: Int): Int {
        // Her 100 puanda 1 seviye atlıyo
        return (points / 100) + 1
    }

    private fun checkBadges(points: Int, currentBadges: List<String>): List<String> {
        val updatedBadges = currentBadges.toMutableList()
        
        if (points >= 100 && !updatedBadges.contains("İlk Başarı")) {
            updatedBadges.add("İlk Başarı") // Sanal Rozet 1
        }
        if (points >= 500 && !updatedBadges.contains("İrade Şampiyonu")) {
            updatedBadges.add("İrade Şampiyonu") // Sanal Rozet 2 (İleride tema fidan bağışına dönüşüyo)
        }
        
        return updatedBadges
    }
}
