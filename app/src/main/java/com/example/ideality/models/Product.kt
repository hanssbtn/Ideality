package com.example.ideality.models

import android.graphics.Bitmap

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val image: Bitmap,
    val modelFile: String,  // AR model file path/reference
    val category: String,
    val rating: Float,
    val reviewCount: Int,
    val isNew: Boolean = false,  // For "New Releases" section
    val lastUsed: Long? = null,  // Timestamp for "Recently Used" section
    var isFavorite: Boolean = false
)
