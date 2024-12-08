package com.example.ideality.models

data class Address(
    val id: String = "",
    val label: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val streetAddress: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val additionalInfo: String = "",
    var isDefault: Boolean = false
) {
    // Convert Address to HashMap for Firebase
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "label" to label,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "streetAddress" to streetAddress,
            "city" to city,
            "state" to state,
            "postalCode" to postalCode,
            "additionalInfo" to additionalInfo,
            "isDefault" to isDefault
        )
    }

    companion object {
        // Create Address from Firebase DataSnapshot
        fun fromMap(id: String, map: Map<String, Any?>): Address {
            return Address(
                id = id,
                label = map["label"] as? String ?: "",
                fullName = map["fullName"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String ?: "",
                streetAddress = map["streetAddress"] as? String ?: "",
                city = map["city"] as? String ?: "",
                state = map["state"] as? String ?: "",
                postalCode = map["postalCode"] as? String ?: "",
                additionalInfo = map["additionalInfo"] as? String ?: "",
                isDefault = map["isDefault"] as? Boolean ?: false
            )
        }
    }
}