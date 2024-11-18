// managers/FirebaseManager.kt
package com.example.ideality.managers

import com.example.ideality.models.Product
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class FirebaseManager {
    private val database = FirebaseDatabase.getInstance()
    private val productsRef = database.getReference("products")

    fun getProduct(
        productId: String,
        onSuccess: (Product?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsRef.child(productId).get()
            .addOnSuccessListener { snapshot ->
                val product = snapshot.getValue(Product::class.java)
                onSuccess(product)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getSimilarProducts(
        category: String,
        excludeProductId: String,
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsRef.orderByChild("category")
            .equalTo(category)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val products = mutableListOf<Product>()
                    snapshot.children.forEach { childSnapshot ->
                        childSnapshot.getValue(Product::class.java)?.let { product ->
                            if (product.id != excludeProductId) {
                                products.add(product)
                            }
                        }
                    }
                    onSuccess(products)
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(error.toException())
                }
            })
    }

    fun addProduct(
        product: Product,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsRef.child(product.id).setValue(product)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateProduct(
        productId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsRef.child(productId).updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsRef.child(productId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}