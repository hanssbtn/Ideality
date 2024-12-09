// models/Rating.kt
package com.example.ideality.models

data class Rating(
    val rating: Float = 0f,
    val review: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "rating" to rating,
        "review" to review,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Rating = Rating(
            rating = (map["rating"] as? Number)?.toFloat() ?: 0f,
            review = map["review"] as? String ?: "",
            timestamp = map["timestamp"] as? Long ?: System.currentTimeMillis()
        )
    }
}