package com.example.ideality.models
data class UserData(
    val uid: String = "",
    var username: String = "",
    val email: String = "",
    var phone: String = "",
    val password: String = "",
    val createdAt: Long = 0,
    val googleLinked: Boolean = false,
    val authType: String = "email",
    var profileImageId: String = "" // Added for Appwrite
)