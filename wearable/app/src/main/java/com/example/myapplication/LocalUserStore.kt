package com.example.myapplication

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

class LocalUserStore(
    private val preferences: SharedPreferences
) {
    private val usersKey = "registered_users"

    fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole
    ): String? {
        val normalizedEmail = email.trim().lowercase()
        val users = readUsers()

        if (users.any { it.email == normalizedEmail }) {
            return "Bu e-posta adresiyle daha önce kayıt oluşturulmuş."
        }

        users.add(
            LocalUser(
                fullName = fullName.trim(),
                email = normalizedEmail,
                passwordHash = hashPassword(password),
                role = role
            )
        )

        saveUsers(users)
        return null
    }

    fun authenticate(
        email: String,
        password: String
    ): LocalUser? {
        val normalizedEmail = email
            .trim()
            .lowercase(Locale.ROOT)
        val passwordHash = hashPassword(password)

        return readUsers().firstOrNull { user ->
            user.email == normalizedEmail &&
                    user.passwordHash == passwordHash
        }
    }

    private fun readUsers(): MutableList<LocalUser> {
        val rawJson = preferences.getString(usersKey, "[]") ?: "[]"

        return try {
            val jsonArray = JSONArray(rawJson)
            val users = mutableListOf<LocalUser>()

            for (index in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(index)

                val role = runCatching {
                    UserRole.valueOf(
                        jsonObject.getString("role")
                    )
                }.getOrDefault(UserRole.PATIENT)

                users.add(
                    LocalUser(
                        fullName = jsonObject.getString("fullName"),
                        email = jsonObject.getString("email"),
                        passwordHash = jsonObject.getString("passwordHash"),
                        role = role
                    )
                )
            }

            users
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun saveUsers(users: List<LocalUser>) {
        val jsonArray = JSONArray()

        users.forEach { user ->
            val jsonObject = JSONObject().apply {
                put("fullName", user.fullName)
                put("email", user.email)
                put("passwordHash", user.passwordHash)
                put("role", user.role.name)
            }

            jsonArray.put(jsonObject)
        }

        preferences
            .edit()
            .putString(usersKey, jsonArray.toString())
            .apply()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }
}