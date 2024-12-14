package com.example.ideality.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ideality.databinding.ItemOrderSummaryBinding
import com.example.ideality.models.CartItem
import java.text.NumberFormat
import java.util.Locale

class OrderItemsAdapter : ListAdapter<CartItem, OrderItemsAdapter.ViewHolder>(CartItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        println("DEBUG: Binding item at position $position: ${item.product?.name}")  // Debug log
        holder.bind(item)
    }

    class ViewHolder(private val binding: ItemOrderSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            println("DEBUG: ViewHolder binding item: ${item.product?.name}")  // Debug log
            item.product?.let { product ->
                binding.apply {
                    productName.text = product.name
                    quantityText.text = "Quantity: ${item.quantity}"
                    priceText.text = formatPrice(product.price * item.quantity)

                    // Add error handling for image loading
                    Glide.with(itemView.context)
                        .load(product.thumbnailUrl)
                        .into(productImage)
                }
            }
        }

        private fun formatPrice(price: Double): String {
            return NumberFormat.getCurrencyInstance(Locale.US).format(price)
        }
    }

    class CartItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean =
            oldItem == newItem
    }
}