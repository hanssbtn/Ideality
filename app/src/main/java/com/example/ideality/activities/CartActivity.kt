package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideality.adapters.CartAdapter
import com.example.ideality.databinding.ActivityCartBinding
import com.example.ideality.models.CartItem
import com.example.ideality.models.Product
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter

    private var cartItems = mutableListOf<CartItem>()
    private val SHIPPING_COST = 10.00  // Fixed shipping cost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        setupViews()
        loadCartItems()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupCheckoutButton()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = emptyList(),
            onQuantityChanged = { cartItem, newQuantity ->
                updateCartItemQuantity(cartItem.id, newQuantity)
            },
            onRemoveClick = { cartItem ->
                showRemoveConfirmation(cartItem)
            }
        )

        binding.cartRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }

    private fun setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener {
            if (cartItems.isEmpty()) {
                showError("Your cart is empty")
                return@setOnClickListener
            }
            startCheckout()
        }
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("cart")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cartItems.clear()

                    // First, get all cart items
                    val tempCartItems = mutableListOf<CartItem>()
                    snapshot.children.forEach { child ->
                        val cartItemMap = child.value as? Map<String, Any?> ?: return@forEach
                        tempCartItems.add(CartItem.fromMap(child.key ?: "", cartItemMap))
                    }

                    // Then load products for each cart item
                    loadCartProducts(tempCartItems)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error loading cart: ${error.message}")
                }
            })
    }

    private fun loadCartProducts(cartItems: List<CartItem>) {
        var loadedItems = 0

        if (cartItems.isEmpty()) {
            updateUI(emptyList())
            return
        }

        cartItems.forEach { cartItem ->
            database.getReference("products")
                .child(cartItem.productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val productMap = snapshot.value as? Map<String, Any?> ?: return
                        val product = Product.fromMap(productMap, snapshot.key ?: "")
                        cartItem.product = product

                        loadedItems++
                        if (loadedItems == cartItems.size) {
                            updateUI(cartItems)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedItems++
                        if (loadedItems == cartItems.size) {
                            updateUI(cartItems)
                        }
                    }
                })
        }
    }

    private fun updateUI(items: List<CartItem>) {
        cartItems = items.toMutableList()
        cartAdapter.updateItems(items)
        updatePrices()
        updateEmptyState()
    }

    private fun updatePrices() {
        val subtotal = cartAdapter.calculateTotal()
        val total = subtotal + SHIPPING_COST

        binding.apply {
            subtotalText.text = formatPrice(subtotal)
            shippingText.text = formatPrice(SHIPPING_COST)
            totalText.text = formatPrice(total)
            bottomTotalText.text = formatPrice(total)

            // Show/hide order summary
            orderSummaryCard.visibility = if (cartItems.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun updateEmptyState() {
        binding.apply {
            emptyStateLayout.visibility = if (cartItems.isEmpty()) View.VISIBLE else View.GONE
            cartRecyclerView.visibility = if (cartItems.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun updateCartItemQuantity(cartItemId: String, newQuantity: Int) {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("cart")
            .child(cartItemId)
            .child("quantity")
            .setValue(newQuantity)
            .addOnFailureListener { e ->
                showError("Failed to update quantity: ${e.message}")
            }
    }

    private fun showRemoveConfirmation(cartItem: CartItem) {
        val productName = cartItem.product?.name ?: "this item"

        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Item")
            .setMessage("Are you sure you want to remove $productName from your cart?")
            .setPositiveButton("Remove") { _, _ ->
                removeFromCart(cartItem.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeFromCart(cartItemId: String) {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("cart")
            .child(cartItemId)
            .removeValue()
            .addOnFailureListener { e ->
                showError("Failed to remove item: ${e.message}")
            }
    }

    private fun startCheckout() {
        // Save cart total in shared preferences or pass via intent
        val total = cartAdapter.calculateTotal() + SHIPPING_COST

        // Navigate to checkout/address selection
        val intent = Intent(this, SavedAddressesActivity::class.java)
        intent.putExtra("checkout_total", total)
        startActivity(intent)
    }

    private fun formatPrice(price: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(price)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "CartActivity"
    }
}