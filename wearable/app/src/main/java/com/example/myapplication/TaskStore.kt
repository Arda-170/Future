package com.example.myapplication

import android.content.SharedPreferences
import java.time.LocalDate

class TaskStore(
    private val preferences: SharedPreferences
) {
    private fun taskKey(
        userEmail: String,
        taskId: String,
        date: LocalDate = LocalDate.now()
    ): String {
        val normalizedEmail = userEmail
            .trim()
            .lowercase()

        return "task_${normalizedEmail}_${date}_$taskId"
    }

    fun isCompleted(
        userEmail: String,
        taskId: String,
        date: LocalDate = LocalDate.now()
    ): Boolean {
        return preferences.getBoolean(
            taskKey(
                userEmail = userEmail,
                taskId = taskId,
                date = date
            ),
            false
        )
    }

    fun markCompleted(
        userEmail: String,
        taskId: String,
        date: LocalDate = LocalDate.now()
    ) {
        preferences
            .edit()
            .putBoolean(
                taskKey(
                    userEmail = userEmail,
                    taskId = taskId,
                    date = date
                ),
                true
            )
            .apply()
    }
}