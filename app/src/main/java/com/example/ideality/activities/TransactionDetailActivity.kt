package com.example.ideality.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.content.DialogInterface
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideality.R
import com.example.ideality.adapter.OrderItemsAdapter
import com.example.ideality.databinding.ActivityTransactionDetailBinding
import com.example.ideality.databinding.DialogRatingBinding
import com.example.ideality.models.CartItem
import com.example.ideality.models.Product
import com.example.ideality.models.Rating
import com.example.ideality.models.Transaction
import com.example.ideality.models.TransactionStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class TransactionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var orderItemsAdapter: OrderItemsAdapter
    private var currentTransaction: Transaction? = null
    private val SHIPPING_COST = 10.00

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val transactionId = intent.getStringExtra("transaction_id") ?: run {
            showError("Transaction not found")
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        loadTransaction(transactionId)
    }

    private fun initializeViews() {
        database = FirebaseDatabase.getInstance()
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        println("DEBUG: Setting up RecyclerView")
        orderItemsAdapter = OrderItemsAdapter()
        binding.orderItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TransactionDetailActivity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            adapter = orderItemsAdapter
            setHasFixedSize(true)

            // Add this post-layout listener
            viewTreeObserver.addOnGlobalLayoutListener {
                println("DEBUG: RecyclerView height: $height")
            }
        }
    }


    private fun loadTransaction(transactionId: String) {
        database.getReference("transactions")
            .child(transactionId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactionMap = snapshot.value as? Map<String, Any?> ?: return
                    currentTransaction = Transaction.fromMap(snapshot.key ?: "", transactionMap)

                    // Load product details for each item
                    currentTransaction?.let { transaction ->
                        loadProductDetails(transaction.items)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error loading transaction: ${error.message}")
                }
            })
    }

    private fun loadProductDetails(items: List<CartItem>) {
        var loadedItems = 0
        val updatedItems = mutableListOf<CartItem>()

        items.forEach { item ->
            database.getReference("products")
                .child(item.productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val productMap = snapshot.value as? Map<String, Any?> ?: return
                        val product = Product.fromMap(productMap, snapshot.key ?: "")
                        val updatedItem = item.copy(product = product)
                        updatedItems.add(updatedItem)

                        loadedItems++
                        if (loadedItems == items.size) {
                            // All products loaded, update the transaction
                            currentTransaction = currentTransaction?.copy(items = updatedItems)
                            updateUI()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedItems++
                        updatedItems.add(item)
                        if (loadedItems == items.size) {
                            currentTransaction = currentTransaction?.copy(items = updatedItems)
                            updateUI()
                        }
                    }
                })
        }
    }

    private fun updateUI() {
        currentTransaction?.let { transaction ->
            println("DEBUG: Updating UI with transaction: ${transaction.id}")
            println("DEBUG: Number of items: ${transaction.items.size}")
            transaction.items.forEach { item ->
                println("DEBUG: Item: ${item.product?.name}, Quantity: ${item.quantity}")
            }

            // Force layout update after submitting list
            orderItemsAdapter.submitList(transaction.items) {
                binding.orderItemsRecyclerView.requestLayout()
            }

            // Update address
            binding.addressDetails.text = buildString {
                append(transaction.shippingAddress.fullName)
                append("\n")
                append(transaction.shippingAddress.phoneNumber)
                append("\n\n")
                append(transaction.shippingAddress.streetAddress)
                append("\n")
                append(transaction.shippingAddress.city)
                append(", ")
                append(transaction.shippingAddress.state)
                append(" ")
                append(transaction.shippingAddress.postalCode)
            }

            // Update price details
            val subtotal = transaction.totalAmount - SHIPPING_COST
            binding.apply {
                subtotalText.text = formatPrice(subtotal)
                shippingText.text = formatPrice(SHIPPING_COST)
                totalText.text = formatPrice(transaction.totalAmount)
            }

            // Update status steps
            updateStatusSteps(transaction.status)

            // Update action button - Fix to properly show Confirm Delivery button
            binding.actionButton.apply {
                when (transaction.status) {
                    TransactionStatus.PROCESSING -> {
                        text = "Confirm Order"
                        setOnClickListener {
                            updateTransactionStatus(transaction.id, TransactionStatus.CONFIRMED)
                        }
                        visibility = View.VISIBLE
                    }
                    TransactionStatus.CONFIRMED -> {
                        text = "Shipping"
                        setOnClickListener {
                            updateTransactionStatus(transaction.id, TransactionStatus.SHIPPING)
                        }
                        visibility = View.VISIBLE
                    }
                    TransactionStatus.SHIPPING -> {
                        text = "Confirm Delivery"
                        setOnClickListener { confirmDelivery(transaction) }
                        visibility = View.VISIBLE
                    }
                    TransactionStatus.DELIVERED -> {
                        if (!transaction.isRated) {
                            text = "Rate Order"
                            setOnClickListener { showRatingDialog(transaction) }
                            visibility = View.VISIBLE
                        } else {
                            visibility = View.GONE
                        }
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateStatusSteps(currentStatus: TransactionStatus) {
        binding.statusStepsLayout.removeAllViews()

        val statusInfo = mapOf(
            TransactionStatus.PROCESSING to Pair(
                R.drawable.ic_processing,
                "Order is being processed"
            ),
            TransactionStatus.CONFIRMED to Pair(
                R.drawable.ic_confirmed,
                "Order has been confirmed"
            ),
            TransactionStatus.SHIPPING to Pair(
                R.drawable.ic_shipping,
                "Order is being shipped"
            ),
            TransactionStatus.DELIVERED to Pair(
                R.drawable.ic_delivered,
                "Order has been delivered"
            ),
            TransactionStatus.COMPLETED to Pair(
                R.drawable.ic_completed,
                "Order completed and rated"
            )
        )

        TransactionStatus.entries.forEach { status ->
            val isCompleted = status.ordinal <= currentStatus.ordinal
            val stepView = layoutInflater.inflate(
                R.layout.item_status_step,
                binding.statusStepsLayout,
                false
            )

            stepView.findViewById<ImageView>(R.id.statusIcon).apply {
                setImageResource(statusInfo[status]?.first ?: R.drawable.ic_processing)
                setColorFilter(
                    if (isCompleted) Color.parseColor("#000000") else Color.parseColor("#757575")
                )
            }

            stepView.findViewById<TextView>(R.id.statusTitle).apply {
                text = status.name
                setTextColor(
                    if (isCompleted) Color.parseColor("#000000") else Color.parseColor("757575")
                )
            }

            stepView.findViewById<TextView>(R.id.statusDescription).apply {
                text = statusInfo[status]?.second
                setTextColor(
                    if (isCompleted) Color.parseColor("#000000") else Color.parseColor("757575")
                )
            }

            binding.statusStepsLayout.addView(stepView)

            // Add connector line if not the last item
            if (status.ordinal < TransactionStatus.entries.size - 1) {
                val lineView = View(this).apply {
                    layoutParams = ViewGroup.LayoutParams(2, 40)
                    setBackgroundColor(
                        if (isCompleted) Color.parseColor("#000000") else Color.parseColor("757575")
                    )
                }
                binding.statusStepsLayout.addView(lineView)
            }
        }
    }

    private fun updateActionButton(transaction: Transaction) {
        binding.actionButton.apply {
            when (transaction.status) {
                TransactionStatus.SHIPPING -> {
                    text = "Confirm Delivery"
                    setOnClickListener { confirmDelivery(transaction) }
                    visibility = View.VISIBLE
                }
                TransactionStatus.DELIVERED -> {
                    if (!transaction.isRated) {
                        text = "Rate Order"
                        setOnClickListener { showRatingDialog(transaction) }
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                    }
                }
                else -> {
                    visibility = View.GONE
                }
            }
        }
    }

    private fun confirmDelivery(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Delivery")
            .setMessage("Have you received your order?")
            .setPositiveButton("Yes") { _, _ ->
                updateTransactionStatus(transaction.id, TransactionStatus.DELIVERED)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showRatingDialog(transaction: Transaction) {
        val dialogBinding = DialogRatingBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Rate Your Order")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit", null) // We'll set the listener later
            .setNegativeButton("Later", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val rating = dialogBinding.ratingBar.rating
                if (rating == 0f) {
                    showError("Please select a rating")
                    return@setOnClickListener
                }

                val review = dialogBinding.reviewInput.text.toString()
                submitRating(transaction, Rating(rating, review))
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun submitRating(transaction: Transaction, rating: Rating) {
        val updates = hashMapOf<String, Any>(
            "transactions/${transaction.id}/isRated" to true,
            "transactions/${transaction.id}/status" to TransactionStatus.COMPLETED.name,
            "transactions/${transaction.id}/rating" to rating.toMap()
        )

        // Update product ratings for each item in the transaction
        transaction.items.forEach { cartItem ->
            cartItem.product?.let { product ->
                val productRef = "products/${product.id}"
                val currentRating = product.rating
                val currentCount = product.reviewCount
                val newCount = currentCount + 1

                // Calculate new average rating
                val newRating = ((currentRating * currentCount) + rating.rating) / newCount

                updates["$productRef/reviewCount"] = newCount
                updates["$productRef/rating"] = newRating
            }
        }

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                showSuccess("Thank you for your rating!")
            }
            .addOnFailureListener { e ->
                showError("Failed to submit rating: ${e.message}")
            }
    }

    private fun updateTransactionStatus(
        transactionId: String,
        newStatus: TransactionStatus
    ) {
        database.getReference("transactions")
            .child(transactionId)
            .child("status")
            .setValue(newStatus.name)
            .addOnSuccessListener {
                showSuccess("Status updated successfully")
            }
            .addOnFailureListener { e ->
                showError("Failed to update status: ${e.message}")
            }
    }

    private fun formatPrice(price: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(price)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}