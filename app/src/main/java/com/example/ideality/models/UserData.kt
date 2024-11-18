package com.example.ideality.models
data class UserData(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val profileImageId: String = "", // For Appwrite storage
    val googleLinked: Boolean = false,
    val authType: String = "email",  // "email", "google", or "both"
    val createdAt: Long = System.currentTimeMillis()
)