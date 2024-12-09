package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ideality.adapters.ProductAdapter
import com.example.ideality.databinding.ActivityCategoryDetailBinding
import com.example.ideality.managers.WishlistManager
import com.example.ideality.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CategoryDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var wishlistManager: WishlistManager
    private lateinit var productAdapter: ProductAdapter

    private var categoryId: String? = null
    private var categoryName: String? = null
    private var listType: String? = null // "category", "recent", or "new"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get intent data
        categoryId = intent.getStringExtra("category_id")
        categoryName = intent.getStringExtra("category_name")
        listType = intent.getStringExtra("list_type")

        initializeFirebase()
        setupViews()
        loadProducts()
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        FirebaseAuth.getInstance().currentUser?.let { user ->
            wishlistManager = WishlistManager(database, user.uid)
        }
    }

    private fun setupViews() {
        // Setup toolbar title based on type
        binding.categoryTitle.text = when (listType) {
            "recent" -> "Recently Used"
            "new" -> "New Releases"
            else -> categoryName ?: "Products"
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup RecyclerView
        productAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { product -> toggleFavorite(product) },
            onQuickArView = { product -> showArView(product) }
        )

        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@CategoryDetailActivity, 2)
            adapter = productAdapter
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener { loadProducts() }
    }

    private fun loadProducts() {
        showLoading(true)

        when (listType) {
            "recent" -> loadRecentlyUsedProducts()
            "new" -> loadNewProducts()
            else -> loadCategoryProducts()
        }
    }

    private fun loadCategoryProducts() {
        database.getReference("products")
            .orderByChild("category")
            .equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    handleProductsSnapshot(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    handleError(error)
                }
            })
    }

    private fun loadRecentlyUsedProducts() {
        database.getReference("products")
            .orderByChild("lastUsed")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val products = mutableListOf<Product>()
                    snapshot.children.forEach { child ->
                        val productMap = child.value as? Map<String, Any?> ?: return@forEach
                        val product = Product.fromMap(productMap, child.key ?: "")
                        if (product.lastUsed != null) {
                            products.add(product)
                        }
                    }
                    showProducts(products.sortedByDescending { it.lastUsed })
                }

                override fun onCancelled(error: DatabaseError) {
                    handleError(error)
                }
            })
    }

    private fun loadNewProducts() {
        database.getReference("products")
            .orderByChild("isNew")
            .equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    handleProductsSnapshot(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    handleError(error)
                }
            })
    }

    private fun handleProductsSnapshot(snapshot: DataSnapshot) {
        val products = mutableListOf<Product>()
        snapshot.children.forEach { child ->
            val productMap = child.value as? Map<String, Any?> ?: return@forEach
            val product = Product.fromMap(productMap, child.key ?: "")
            products.add(product)
        }
        showProducts(products)
    }

    private fun handleError(error: DatabaseError) {
        showLoading(false)
        showError("Error loading products: ${error.message}")
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            shimmerLayout.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                shimmerLayout.startShimmer()
                productsRecyclerView.visibility = View.GONE
                emptyStateLayout.visibility = View.GONE
            } else {
                shimmerLayout.stopShimmer()
            }
            swipeRefresh.isRefreshing = false
        }
    }

    private fun showEmptyState() {
        binding.apply {
            productsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE

            // Set appropriate empty state message
            emptyStateMessage.text = when (listType) {
                "recent" -> "You haven't viewed any products yet"
                "new" -> "No new products available"
                else -> "No products available in this category"
            }
        }
    }

    private fun showProducts(products: List<Product>) {
        showLoading(false)
        if (products.isEmpty()) {
            showEmptyState()
        } else {
            binding.apply {
                productsRecyclerView.visibility = View.VISIBLE
                emptyStateLayout.visibility = View.GONE
            }
            productAdapter.updateProducts(products)
        }
    }

    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("product_id", product.id)
        }
        startActivity(intent)
    }

    private fun showArView(product: Product) {
        val intent = Intent(this, ARViewerActivity::class.java).apply {
            putExtra("modelUrl", product.modelUrl)
            putExtra("productId", product.id)
        }
        startActivity(intent)

        // Update lastUsed timestamp
        database.getReference("products")
            .child(product.id)
            .child("lastUsed")
            .setValue(System.currentTimeMillis())
    }

    private fun toggleFavorite(product: Product) {
        if (product.isFavorite) {
            wishlistManager.removeFromWishlist(
                product.id,
                onSuccess = {
                    showSuccess("Removed from wishlist")
                },
                onFailure = { e ->
                    showError("Error: ${e.message}")
                }
            )
        } else {
            wishlistManager.addToWishlist(
                product,
                onSuccess = {
                    showSuccess("Added to wishlist")
                },
                onFailure = { e ->
                    showError("Error: ${e.message}")
                }
            )
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        binding.shimmerLayout.stopShimmer()
    }

    override fun onResume() {
        super.onResume()
        if (binding.shimmerLayout.visibility == View.VISIBLE) {
            binding.shimmerLayout.startShimmer()
        }
    }
}