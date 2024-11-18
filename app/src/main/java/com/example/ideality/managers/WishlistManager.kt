package com.example.ideality.managers

import com.example.ideality.models.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WishlistManager(private val database: FirebaseDatabase, private val userId: String) {
    private val wishlistRef: DatabaseReference = database.getReference("wishlists").child(userId)

    fun addToWishlist(product: Product, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        wishlistRef.child(product.id).setValue(product)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun removeFromWishlist(productId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        wishlistRef.child(productId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun getWishlist(onSuccess: (List<Product>) -> Unit, onFailure: (Exception) -> Unit) {
        wishlistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.mapNotNull { dataSnapshot ->
                    dataSnapshot.getValue(Product::class.java)
                }
                onSuccess(products)
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.toException())
            }
        })
    }

    // Optional: Add a method to check if a product is in wishlist
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

    // Optional: Add a method to observe wishlist changes
    fun observeWishlist(onUpdate: (List<Product>) -> Unit, onError: (Exception) -> Unit) {
        wishlistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.mapNotNull { dataSnapshot ->
                    dataSnapshot.getValue(Product::class.java)
                }
                onUpdate(products)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }
}