package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ideality.adapters.OrderItemsAdapter
import com.example.ideality.databinding.ActivityCheckoutBinding
import com.example.ideality.databinding.ItemOrderSummaryBinding
import com.example.ideality.models.Address
import com.example.ideality.models.CartItem
import com.example.ideality.models.Product
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import com.example.ideality.models.Transaction
import com.example.ideality.models.TransactionStatus
import java.util.Locale
import java.util.UUID
class CheckoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var orderItemsAdapter: OrderItemsAdapter

    private var defaultAddress: Address? = null
    private var cartItems = mutableListOf<CartItem>()
    private val SHIPPING_COST = 10.00

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        setupViews()
        loadDefaultAddress()
        loadCartItems()
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemsAdapter()
        binding.orderItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            adapter = orderItemsAdapter
        }
    }

    private fun setupButtons() {
        binding.apply {
            changeAddressButton.setOnClickListener {
                startActivity(Intent(this@CheckoutActivity, SavedAddressesActivity::class.java))
            }

            proceedToPaymentButton.setOnClickListener {
                validateAndProceedToPayment()
            }
        }
    }

    private fun loadDefaultAddress() {
        val userId = auth.currentUser?.uid ?: run {
            showError("Please sign in to continue")
            finish()
            return
        }

        database.getReference("users")
            .child(userId)
            .child("addresses")
            .orderByChild("isDefault")
            .equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showError("Please add a shipping address")
                        startActivity(Intent(this@CheckoutActivity, SavedAddressesActivity::class.java))
                        finish()
                        return
                    }

                    snapshot.children.firstOrNull()?.let { child ->
                        val addressMap = child.value as? Map<String, Any?> ?: return
                        defaultAddress = Address.fromMap(child.key ?: "", addressMap)
                        updateAddressUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error loading address: ${error.message}")
                }
            })
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("cart")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cartItems.clear()

                    val tempCartItems = mutableListOf<CartItem>()
                    snapshot.children.forEach { child ->
                        val cartItemMap = child.value as? Map<String, Any?> ?: return@forEach
                        tempCartItems.add(CartItem.fromMap(child.key ?: "", cartItemMap))
                    }

                    loadCartProducts(tempCartItems)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error loading cart: ${error.message}")
                }
            })
    }

    private fun loadCartProducts(items: List<CartItem>) {
        var loadedCount = 0
        if (items.isEmpty()) {
            showError("Your cart is empty")
            finish()
            return
        }

        items.forEach { cartItem ->
            database.getReference("products")
                .child(cartItem.productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val productMap = snapshot.value as? Map<String, Any?> ?: return
                        val product = Product.fromMap(productMap, snapshot.key ?: "")
                        cartItem.product = product

                        loadedCount++
                        if (loadedCount == items.size) {
                            cartItems = items.toMutableList()
                            updateOrderSummary()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == items.size) {
                            updateOrderSummary()
                        }
                    }
                })
        }
    }

    private fun updateAddressUI() {
        defaultAddress?.let { address ->
            binding.apply {
                addressLabel.text = address.label
                fullName.text = address.fullName
                fullAddress.text = buildString {
                    append(address.streetAddress)
                    append(", ")
                    append(address.city)
                    append(", ")
                    append(address.state)
                    append(" ")
                    append(address.postalCode)
                }
                // Use phoneNumber instead of phone to match your Address model
                phoneNumber.text = address.phoneNumber
            }
        }
    }

    private fun updateOrderSummary() {
        orderItemsAdapter.submitList(cartItems)

        val subtotal = calculateSubtotal()
        val total = subtotal + SHIPPING_COST

        binding.apply {
            subtotalText.text = formatPrice(subtotal)
            shippingText.text = formatPrice(SHIPPING_COST)
            totalText.text = formatPrice(total)
        }
    }

    private fun calculateSubtotal(): Double {
        return cartItems.sumOf { item ->
            (item.product?.price ?: 0.0) * item.quantity
        }
    }

    private fun validateAndProceedToPayment() {
        val userId = auth.currentUser?.uid ?: return

        if (defaultAddress == null) {
            showError("Please select a shipping address")
            return
        }

        if (cartItems.isEmpty()) {
            showError("Your cart is empty")
            return
        }

        // Create new transaction
        val transactionId = UUID.randomUUID().toString()
        val total = calculateSubtotal() + SHIPPING_COST

        val transaction = Transaction(
            id = transactionId,
            userId = userId,
            items = cartItems.toList(),
            status = TransactionStatus.PROCESSING,
            totalAmount = total,
            shippingAddress = defaultAddress!!,
            timestamp = System.currentTimeMillis()
        )

        // Create a map of updates to perform
        val updates = mutableMapOf<String, Any?>()

        // Add transaction
        updates["transactions/$transactionId"] = transaction.toMap()

        // Clear cart items
        cartItems.forEach { cartItem ->
            updates["users/$userId/cart/${cartItem.id}"] = null
        }

        // Perform all updates atomically
        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                showSuccess("Order placed successfully!")
                // Navigate to transaction detail
                startActivity(Intent(this, TransactionDetailActivity::class.java).apply {
                    putExtra("transaction_id", transactionId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                finish()
            }
            .addOnFailureListener { e ->
                showError("Failed to place order: ${e.message}")
            }
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPaymentSuccessDialog(orderId: String, total: Double) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Order Placed Successfully")
            .setMessage("Your order (ID: $orderId) has been placed successfully.\nTotal Amount: ${formatPrice(total)}")
            .setPositiveButton("View Orders") { _, _ ->
                // Navigate to orders screen
                // startActivity(Intent(this, OrdersActivity::class.java))
                finish()
            }
            .setNegativeButton("Continue Shopping") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun formatPrice(price: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(price)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}