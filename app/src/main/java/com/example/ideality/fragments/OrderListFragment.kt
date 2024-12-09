package com.example.ideality.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideality.activities.TransactionDetailActivity
import com.example.ideality.adapters.OrderAdapter
import com.example.ideality.databinding.FragmentOrderListBinding
import com.example.ideality.models.*
import com.example.ideality.databinding.DialogRatingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.content.DialogInterface
import com.example.ideality.models.Transaction

class OrderListFragment : Fragment() {
    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = _binding!!
    private var isCompleted = false

    private lateinit var orderAdapter: OrderAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCompleted = arguments?.getBoolean(ARG_IS_COMPLETED) ?: false

        initializeFirebase()
        setupRecyclerView()
        setupEmptyState()
        loadOrders()
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onItemClick = { transaction ->
                val intent = Intent(requireContext(), TransactionDetailActivity::class.java)
                intent.putExtra("transaction_id", transaction.id)
                startActivity(intent)
            }
        )

        binding.ordersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadOrders() {
        showLoading(true)
        val userId = auth.currentUser?.uid ?: return

        database.getReference("transactions")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val orders = mutableListOf<Transaction>()

                    snapshot.children.forEach { child ->
                        val transaction = Transaction.fromMap(
                            child.key ?: "",
                            child.value as? Map<String, Any?> ?: return@forEach
                        )

                        // Filter based on status
                        val isTransactionCompleted = transaction.status == TransactionStatus.COMPLETED
                        if (isTransactionCompleted == isCompleted) {
                            orders.add(transaction)
                        }
                    }

                    if (orders.isEmpty()) {
                        showLoading(false)
                        showEmptyState()
                    } else {
                        // Load product details for all transactions
                        loadProductDetailsForTransactions(orders.sortedByDescending { it.timestamp })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Error loading orders: ${error.message}")
                }
            })
    }

    private fun loadProductDetailsForTransactions(transactions: List<Transaction>) {
        var loadedTransactions = 0
        val updatedTransactions = mutableListOf<Transaction>()

        transactions.forEach { transaction ->
            loadProductDetailsForTransaction(transaction) { updatedTransaction ->
                updatedTransactions.add(updatedTransaction)
                loadedTransactions++

                if (loadedTransactions == transactions.size) {
                    // All transactions loaded with product details
                    showLoading(false)
                    showOrders(updatedTransactions.sortedByDescending { it.timestamp })
                }
            }
        }
    }

    private fun loadProductDetailsForTransaction(transaction: Transaction, onComplete: (Transaction) -> Unit) {
        var loadedItems = 0
        val updatedItems = mutableListOf<CartItem>()

        transaction.items.forEach { item ->
            database.getReference("products")
                .child(item.productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val productMap = snapshot.value as? Map<String, Any?> ?: return
                        val product = Product.fromMap(productMap, snapshot.key ?: "")
                        val updatedItem = item.copy(product = product)
                        updatedItems.add(updatedItem)

                        loadedItems++
                        if (loadedItems == transaction.items.size) {
                            // All products loaded for this transaction
                            onComplete(transaction.copy(items = updatedItems))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedItems++
                        updatedItems.add(item)
                        if (loadedItems == transaction.items.size) {
                            onComplete(transaction.copy(items = updatedItems))
                        }
                    }
                })
        }
    }

    private fun showConfirmDeliveryDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rate Your Order")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit", null)
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

        // Update product ratings
        transaction.items.forEach { cartItem ->
            cartItem.product?.let { product ->
                val productRef = "products/${product.id}"
                updates["$productRef/ratingCount"] = product.reviewCount + 1
                updates["$productRef/rating"] =
                    ((product.rating * product.reviewCount + rating.rating) / (product.reviewCount + 1))
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

    private fun setupEmptyState() {
        binding.apply {
            emptyStateTitle.text = if (isCompleted) "No completed orders" else "No ongoing orders"
            emptyStateMessage.text = if (isCompleted) {
                "Your completed orders will appear here"
            } else {
                "Your ongoing orders will appear here"
            }
        }
    }

    private fun showOrders(orders: List<Transaction>) {
        binding.apply {
            ordersRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
        orderAdapter.submitList(orders)
    }

    private fun showEmptyState() {
        binding.apply {
            ordersRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            shimmerLayout.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                shimmerLayout.startShimmer()
                ordersRecyclerView.visibility = View.GONE
                emptyStateLayout.visibility = View.GONE
            } else {
                shimmerLayout.stopShimmer()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IS_COMPLETED = "is_completed"

        fun newInstance(isCompleted: Boolean) = OrderListFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_COMPLETED, isCompleted)
            }
        }
    }
}