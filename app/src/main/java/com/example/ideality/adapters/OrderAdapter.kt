// adapters/OrderAdapter.kt
package com.example.ideality.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ideality.databinding.ItemTransactionBinding
import com.example.ideality.models.Transaction
import com.example.ideality.models.TransactionStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onActionButtonClick: (Transaction) -> Unit
) : ListAdapter<Transaction, OrderAdapter.ViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // Set order ID and date
                orderIdText.text = "Order #${transaction.id.takeLast(8)}"
                dateText.text = formatDate(transaction.timestamp)

                // Set status badge
                statusBadge.text = transaction.status.name
                statusBadge.setBackgroundColor(getStatusColor(transaction.status))

                // Set first item preview
                transaction.items.firstOrNull()?.let { firstItem ->
                    firstItem.product?.let { product ->
                        Glide.with(itemView)
                            .load(product.thumbnailUrl)
                            .into(productImage)

                        val otherItems = transaction.items.size - 1
                        itemsText.text = if (otherItems > 0) {
                            "${product.name} +$otherItems more"
                        } else {
                            product.name
                        }
                    }
                }

                // Set total
                totalText.text = formatPrice(transaction.totalAmount)

                // Set button states
                setupButtons(transaction)

                // Click listeners
                root.setOnClickListener { onItemClick(transaction) }
                actionButton.setOnClickListener { onActionButtonClick(transaction) }
            }
        }

        private fun setupButtons(transaction: Transaction) {
            binding.actionButton.apply {
                when (transaction.status) {
                    TransactionStatus.SHIPPING -> {
                        text = "Confirm Delivery"
                        isEnabled = true
                    }
                    TransactionStatus.DELIVERED -> {
                        if (!transaction.isRated) {
                            text = "Rate Order"
                            isEnabled = true
                        } else {
                            text = "Rated"
                            isEnabled = false
                        }
                    }
                    else -> {
                        text = transaction.status.name
                        isEnabled = false
                    }
                }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        private fun formatPrice(price: Double): String {
            return NumberFormat.getCurrencyInstance(Locale.US).format(price)
        }

        private fun getStatusColor(status: TransactionStatus): Int {
            return when (status) {
                TransactionStatus.PROCESSING -> 0xFF9E9E9E.toInt()  // Grey
                TransactionStatus.CONFIRMED -> 0xFF2196F3.toInt()   // Blue
                TransactionStatus.SHIPPING -> 0xFFFFA000.toInt()    // Amber
                TransactionStatus.DELIVERED -> 0xFF4CAF50.toInt()   // Green
                TransactionStatus.COMPLETED -> 0xFF4CAF50.toInt()   // Green
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem == newItem
    }
}