// models/Transaction.kt
package com.example.ideality.models

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val status: TransactionStatus = TransactionStatus.PROCESSING,
    val totalAmount: Double = 0.0,
    val shippingAddress: Address = Address(),
    val timestamp: Long = System.currentTimeMillis(),
    val isRated: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "items" to items.map { it.toMap() },
            "status" to status.name,
            "totalAmount" to totalAmount,
            "shippingAddress" to shippingAddress.toMap(),
            "timestamp" to timestamp,
            "isRated" to isRated
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Transaction {
            return Transaction(
                id = id,
                userId = map["userId"] as? String ?: "",
                items = (map["items"] as? List<*>)?.mapNotNull {
                    if (it is Map<*, *>) CartItem.fromMap("", it as Map<String, Any?>)
                    else null
                } ?: emptyList(),
                status = TransactionStatus.valueOf(map["status"] as? String ?: TransactionStatus.PROCESSING.name),
                totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                shippingAddress = (map["shippingAddress"] as? Map<String, Any?>)?.let {
                    Address.fromMap("", it)
                } ?: Address(),
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                isRated = (map["isRated"] as? Boolean) ?: false
            )
        }
    }
}