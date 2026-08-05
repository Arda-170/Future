package com.example.myapplication

data class LocalUser(
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole
)