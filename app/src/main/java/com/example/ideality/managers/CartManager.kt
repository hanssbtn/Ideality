package com.example.ideality.managers


import com.example.ideality.models.CartItem
import com.example.ideality.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// managers/CartManager.kt
class CartManager(private val database: FirebaseDatabase) {
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val cartRef = database.getReference("carts")

    fun addToCart(product: Product, quantity: Int = 1, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        currentUser?.let { user ->
            val userCartRef = cartRef.child(user.uid)
            val cartItem = CartItem(
                id = "${product.id}_${System.currentTimeMillis()}",
                productId = product.id,
                quantity = quantity,
                productName = product.name,
                productImage = product.image,
                productPrice = product.price

            )

            userCartRef.child(cartItem.id).setValue(cartItem)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }
    }

    fun removeFromCart(cartItemId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        currentUser?.let { user ->
            cartRef.child(user.uid).child(cartItemId).removeValue()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }
    }

    fun updateQuantity(cartItemId: String, newQuantity: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        currentUser?.let { user ->
            cartRef.child(user.uid).child(cartItemId).child("quantity").setValue(newQuantity)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }
    }

    fun getCartItems(onSuccess: (List<CartItem>) -> Unit, onFailure: (Exception) -> Unit) {
        currentUser?.let { user ->
            cartRef.child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cartItems = mutableListOf<CartItem>()
                    for (itemSnapshot in snapshot.children) {
                        itemSnapshot.getValue(CartItem::class.java)?.let {
                            cartItems.add(it)
                        }
                    }
                    onSuccess(cartItems)
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(error.toException())
                }
            })
        }
    }

    fun observeCartItems(onItemsChanged: (List<CartItem>) -> Unit, onError: (Exception) -> Unit) {
        currentUser?.let { user ->
            cartRef.child(user.uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cartItems = mutableListOf<CartItem>()
                    for (itemSnapshot in snapshot.children) {
                        itemSnapshot.getValue(CartItem::class.java)?.let {
                            cartItems.add(it)
                        }
                    }
                    onItemsChanged(cartItems)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.toException())
                }
            })
        }
    }

    fun getCartTotal(onSuccess: (Double) -> Unit, onFailure: (Exception) -> Unit) {
        getCartItems(
            onSuccess = { items ->
                val total = items.sumOf { it.productPrice * it.quantity }
                onSuccess(total)
            },
            onFailure = onFailure
        )
    }

    fun clearCart(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        currentUser?.let { user ->
            cartRef.child(user.uid).removeValue()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }
    }
}