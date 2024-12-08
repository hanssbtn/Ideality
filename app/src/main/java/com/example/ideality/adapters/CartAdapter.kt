package com.example.ideality.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ideality.databinding.ItemCartBinding
import com.example.ideality.models.CartItem
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var cartItems: List<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onRemoveClick: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            val product = cartItem.product ?: return

            binding.apply {
                // Load product image
                Glide.with(productImage)
                    .load(product.thumbnailUrl)
                    .into(productImage)

                // Set product info
                productName.text = product.name
                productPrice.text = formatPrice(product.price)
                quantityText.text = cartItem.quantity.toString()

                // Handle quantity changes
                decreaseButton.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        onQuantityChanged(cartItem, cartItem.quantity - 1)
                    }
                }

                increaseButton.setOnClickListener {
                    onQuantityChanged(cartItem, cartItem.quantity + 1)
                }

                // Handle remove
                removeButton.setOnClickListener {
                    onRemoveClick(cartItem)
                }

                // Update button states
                decreaseButton.isEnabled = cartItem.quantity > 1
            }
        }

        private fun formatPrice(price: Double): String {
            return NumberFormat.getCurrencyInstance(Locale.US).format(price)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount() = cartItems.size

    fun updateItems(newItems: List<CartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }

    // Helper method to calculate total price
    fun calculateTotal(): Double {
        return cartItems.sumOf { it.product?.price?.times(it.quantity) ?: 0.0 }
    }

    // Helper method to get total items count
    fun getTotalItems(): Int {
        return cartItems.sumOf { it.quantity }
    }
}