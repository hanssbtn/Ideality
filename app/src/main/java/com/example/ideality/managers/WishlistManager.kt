package com.example.ideality.managers

import com.example.ideality.models.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WishlistManager(private val database: FirebaseDatabase, private val userId: String) {
    private val wishlistRef = database.getReference("users").child(userId).child("wishlist")

    fun addToWishlist(product: Product, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        wishlistRef
            .child(product.id)
            .setValue(true)  // Using true instead of the whole product
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun removeFromWishlist(productId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        wishlistRef
            .child(productId)
            .removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Method to check if a product is in wishlist
    fun isInWishlist(productId: String, onResult: (Boolean) -> Unit) {
        wishlistRef.child(productId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onResult(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(false)
            }
        })
    }

    // Method to observe wishlist changes
    fun observeWishlist(onUpdate: (List<String>) -> Unit, onError: (Exception) -> Unit) {
        wishlistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productIds = snapshot.children.mapNotNull { dataSnapshot ->
                    if (dataSnapshot.getValue(Boolean::class.java) == true) {
                        dataSnapshot.key
                    } else {
                        null
                    }
                }
                onUpdate(productIds)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }

    // Helper method to add product to wishlist without callbacks
    fun addToWishlist(product: Product) {
        addToWishlist(product, {}, {})
    }

    // Helper method to remove product from wishlist without callbacks
    fun removeFromWishlist(productId: String) {
        removeFromWishlist(productId, {}, {})
    }
}