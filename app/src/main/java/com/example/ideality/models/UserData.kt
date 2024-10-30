package com.example.ideality.models

data class UserData(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",  // This might be empty for Google sign-in
    val createdAt: Long = System.currentTimeMillis()
)