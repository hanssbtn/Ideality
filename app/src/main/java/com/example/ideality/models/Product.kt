package com.example.ideality.models

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    // Support both local resource and URL
    val image: Int = 0,                // For existing local resource images
    val thumbnailUrl: String = "",     // For cloud stored images
    val modelUrl: String = "",         // AR model URL from Appwrite
    val modelId: String = "",          // Appwrite file ID
    val category: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val isNew: Boolean = false,        // For "New Releases" section
    val lastUsed: Long? = null,        // Timestamp for "Recently Used" section
    var isFavorite: Boolean = false,
    val dimensions: Map<String, Double> = mapOf() // For product dimensions
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "price" to price,
            "thumbnailUrl" to thumbnailUrl,
            "modelUrl" to modelUrl,
            "modelId" to modelId,
            "category" to category,
            "rating" to rating,
            "reviewCount" to reviewCount,
            "isNew" to isNew,
            "lastUsed" to lastUsed,
            "isFavorite" to isFavorite,
            "dimensions" to dimensions
        )
    }
)

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): Product {
            return Product(
                id = id,
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                price = (map["price"] as? Double) ?: 0.0,
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                modelUrl = map["modelUrl"] as? String ?: "",
                modelId = map["modelId"] as? String ?: "",
                category = map["category"] as? String ?: "",
                rating = (map["rating"] as? Double)?.toFloat() ?: 0f,
                reviewCount = (map["reviewCount"] as? Long)?.toInt() ?: 0,
                isNew = map["isNew"] as? Boolean ?: false,
                lastUsed = map["lastUsed"] as? Long,
                isFavorite = map["isFavorite"] as? Boolean ?: false,
                dimensions = (map["dimensions"] as? Map<String, Double>) ?: mapOf()
            )
        }
    }
}