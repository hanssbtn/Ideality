package com.example.ideality.models

// Furniture model data class
data class FurnitureModel(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val modelUrl: String = "",  // Appwrite file URL
    val modelId: String = "",   // Appwrite file ID
    val thumbnailUrl: String = "", // Preview image URL
    val dimensions: Map<String, Double> = mapOf(), // height, width, depth
    val inStock: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "category" to category,
            "description" to description,
            "price" to price,
            "modelUrl" to modelUrl,
            "modelId" to modelId,
            "thumbnailUrl" to thumbnailUrl,
            "dimensions" to dimensions,
            "inStock" to inStock,
            "timestamp" to timestamp
        )
    }
}