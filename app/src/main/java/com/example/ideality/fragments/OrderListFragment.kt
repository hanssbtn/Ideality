// fragments/OrderListFragment.kt
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
import com.example.ideality.models.Transaction
import com.example.ideality.models.TransactionStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
                navigateToDetail(transaction)
            },
            onActionButtonClick = { transaction ->
                handleActionButton(transaction)
            }
        )

        binding.ordersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderAdapter
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

                    showLoading(false)
                    if (orders.isEmpty()) {
                        showEmptyState()
                    } else {
                        showOrders(orders.sortedByDescending { it.timestamp })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Error loading orders: ${error.message}")
                }
            })
    }

    private fun handleActionButton(transaction: Transaction) {
        when (transaction.status) {
            TransactionStatus.DELIVERED -> {
                showRatingDialog(transaction)
            }
            TransactionStatus.SHIPPING -> {
                confirmDelivery(transaction)
            }
            else -> {
                // Handle other statuses if needed
            }
        }
    }

    private fun confirmDelivery(transaction: Transaction) {
        database.getReference("transactions")
            .child(transaction.id)
            .child("status")
            .setValue(TransactionStatus.DELIVERED.name)
            .addOnSuccessListener {
                showSuccess("Delivery confirmed")
            }
            .addOnFailureListener { e ->
                showError("Failed to confirm delivery: ${e.message}")
            }
    }

    private fun showRatingDialog(transaction: Transaction) {
        // We'll implement this later with the rating functionality
    }

    private fun navigateToDetail(transaction: Transaction) {
        val intent = Intent(requireContext(), TransactionDetailActivity::class.java)
        intent.putExtra("transaction_id", transaction.id)
        startActivity(intent)
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