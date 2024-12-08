package com.example.ideality.models

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val quantity: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    var product: Product? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "productId" to productId,
            "quantity" to quantity,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): CartItem {
            return CartItem(
                id = id,
                productId = map["productId"] as? String ?: "",
                quantity = (map["quantity"] as? Long)?.toInt() ?: 1,
                timestamp = map["timestamp"] as? Long ?: System.currentTimeMillis()
            )
        }
    }
}