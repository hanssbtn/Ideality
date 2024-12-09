// models/TransactionStatus.kt
package com.example.ideality.models

enum class TransactionStatus {
    PROCESSING,    // Initial state after payment
    CONFIRMED,     // Order confirmed
    SHIPPING,      // Package is being shipped
    DELIVERED,     // Package has arrived
    COMPLETED      // Transaction finished and rated
}