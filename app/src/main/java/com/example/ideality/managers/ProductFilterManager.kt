package com.example.ideality.managers

import com.example.ideality.models.Product

class ProductFilterManager {
    data class FilterCriteria(
        var minPrice: Double? = null,
        var maxPrice: Double? = null,
        var category: String? = null,
        var isNewOnly: Boolean = false,
        var minRating: Float? = null,
        var sortBy: SortType = SortType.NONE
    )

    enum class SortType {
        NONE,
        PRICE_LOW_TO_HIGH,
        PRICE_HIGH_TO_LOW,
        RATING,
        NEWEST
    }

    fun filterProducts(products: List<Product>, criteria: FilterCriteria): List<Product> {
        return products.filter { product ->
            val meetsMinPrice = criteria.minPrice?.let { product.price >= it } ?: true
            val meetsMaxPrice = criteria.maxPrice?.let { product.price <= it } ?: true
            val meetsCategory = criteria.category?.let { product.category == it } ?: true
            val meetsRating = criteria.minRating?.let { product.rating >= it } ?: true
            val meetsNewOnly = !criteria.isNewOnly || product.isNew

            meetsMinPrice && meetsMaxPrice && meetsCategory && meetsRating && meetsNewOnly
        }.let { filtered ->
            when (criteria.sortBy) {
                SortType.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
                SortType.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
                SortType.RATING -> filtered.sortedByDescending { it.rating }
                SortType.NEWEST -> filtered.sortedByDescending { it.lastUsed ?: 0L } // Using lastUsed for newest sorting
                SortType.NONE -> filtered
            }
        }
    }
}