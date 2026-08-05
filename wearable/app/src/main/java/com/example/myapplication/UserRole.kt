package com.example.myapplication

enum class UserRole(
    val title: String
) {
    PATIENT("Hasta"),
    DOCTOR("Doktor"),
    RELATIVE("Hasta Yakını"),
    ADMIN("Admin")
}