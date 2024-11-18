package com.example.ideality.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ideality.R
import com.example.ideality.models.Product
import com.facebook.shimmer.ShimmerFrameLayout
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit,
    private val onQuickArView: (Product) -> Unit? = {}
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.productImage)
        private val name: TextView = view.findViewById(R.id.productName)
        private val price: TextView = view.findViewById(R.id.productPrice)
        private val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        private val ratingCount: TextView = view.findViewById(R.id.ratingCount)
        private val favoriteButton: ImageButton = view.findViewById(R.id.favoriteButton)
        // Changed from Chip to TextView
        private val newBadge: TextView = view.findViewById(R.id.newBadge)
        private val shimmerLayout: ShimmerFrameLayout? = view.findViewById(R.id.shimmerLayout)
        private val quickArButton: ImageButton = view.findViewById(R.id.quickArButton)

        fun bind(product: Product) {
            // Start shimmer while loading
            shimmerLayout?.startShimmer()

            // Load image and stop shimmer when done
            image.setImageResource(product.image)
            shimmerLayout?.stopShimmer()
            shimmerLayout?.hideShimmer()

            name.text = product.name
            price.text = NumberFormat.getCurrencyInstance(Locale.US).format(product.price)
            ratingBar.rating = product.rating
            ratingCount.text = "(${product.reviewCount})"

            // Set favorite icon with animation
            favoriteButton.setImageResource(
                if (product.isFavorite) R.drawable.ic_favorite
                else R.drawable.ic_favorite
            )

            newBadge.visibility = if (product.isNew) View.VISIBLE else View.GONE

            // Setup click listeners with animations
            itemView.setOnClickListener {
                itemView.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.scale_click))
                onProductClick(product)
            }

            favoriteButton.setOnClickListener {
                val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.favorite_bounce)
                favoriteButton.startAnimation(animation)
                onFavoriteClick(product)
                notifyItemChanged(adapterPosition)
            }

            quickArButton.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.scale_click))
                onQuickArView(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts.toMutableList()
        notifyDataSetChanged()
    }

    fun updateProduct(product: Product) {
        val position = products.indexOfFirst { it.id == product.id }
        if (position != -1) {
            (products as MutableList<Product>)[position] = product
            notifyItemChanged(position)
        }
    }

    fun addProduct(product: Product) {
        (products as MutableList<Product>).add(0, product)
        notifyItemInserted(0)
    }

    fun removeProduct(product: Product) {
        val position = products.indexOf(product)
        if (position != -1) {
            (products as MutableList<Product>).removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateFavoriteStatus(productId: String, isFavorite: Boolean) {
        val position = products.indexOfFirst { it.id == productId }
        if (position != -1) {
            (products as MutableList<Product>)[position] = products[position].copy(isFavorite = isFavorite)
            notifyItemChanged(position)
        }
    }
}