package com.example.ideality.models

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val quantity: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    // We'll store some product info for quick access
    val productName: String = "",
    val productImage: Int = 0,
    val productPrice: Double = 0.0
)