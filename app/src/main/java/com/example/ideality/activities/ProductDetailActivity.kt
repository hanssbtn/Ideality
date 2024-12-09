package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ideality.R
import com.example.ideality.adapters.ProductAdapter
import com.example.ideality.databinding.ActivityProductDetailBinding
import com.example.ideality.managers.WishlistManager
import com.example.ideality.models.CartItem
import com.example.ideality.models.Product
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var wishlistManager: WishlistManager
    private lateinit var similarProductsAdapter: ProductAdapter

    private var currentProduct: Product? = null
    private var currentQuantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productId = intent.getStringExtra("product_id") ?: run {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeFirebase()
        setupViews()
        loadProduct(productId)
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { user ->
            wishlistManager = WishlistManager(database, user.uid)
        }
    }

    private fun setupViews() {
        setupToolbar()
        setupQuantitySelector()
        setupButtons()
        setupSimilarProducts()
    }

    private fun setupToolbar() {
        with(binding) {
            toolbar.apply {
                backButton.setOnClickListener { finish() }
                shareButton.setOnClickListener { shareProduct() }
                favoriteButton.setOnClickListener { toggleFavorite() }
            }
        }
    }

    private fun setupQuantitySelector() {
        with(binding) {
            quantitySelector.apply {
                decreaseButton.setOnClickListener {
                    if (currentQuantity > 1) {
                        currentQuantity--
                        updateQuantityUI()
                    }
                }

                increaseButton.setOnClickListener {
                    currentQuantity++
                    updateQuantityUI()
                }
            }
        }
        updateQuantityUI()
    }

    private fun updateQuantityUI() {
        binding.quantitySelector.quantityText.text = currentQuantity.toString()
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        currentProduct?.let { product ->
            val total = product.price * currentQuantity
            binding.addToCartButton.text = "Add to Cart - $${String.format(resources.configuration.locales[0] ,"%.2f", total)}"
        }
    }

    private fun setupButtons() {
        with(binding) {
            arButton.setOnClickListener {
                currentProduct?.let { showArView(it) }
            }

            addToCartButton.setOnClickListener {
                addToCart()
            }
        }
    }

    private fun setupSimilarProducts() {
        similarProductsAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { navigateToProduct(it) },
            onFavoriteClick = { toggleProductFavorite(it) }
        )

        binding.similarProductsRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@ProductDetailActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = similarProductsAdapter
        }
    }

    private fun loadProduct(productId: String) {
        database.getReference("products").child(productId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val productMap = snapshot.value as? Map<String, Any?> ?: return
                    val product = Product.fromMap(productMap, snapshot.key ?: "")

                    // Check wishlist status when product loads
                    wishlistManager.isInWishlist(productId) { isInWishlist ->
                        currentProduct = product.copy(isFavorite = isInWishlist)
                        updateUI()
                        loadSimilarProducts()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Error loading product: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateUI() {
        currentProduct?.let { product ->
            with(binding) {
                Glide.with(this@ProductDetailActivity)
                    .load(product.thumbnailUrl)
                    .into(productImage)

                productName.text = product.name
                productPrice.text = "$${String.format(resources.configuration.locales[0],"%.2f", product.price)}"
                productDescription.text = product.description
                ratingBar.rating = product.rating
                ratingCount.text = "(${product.reviewCount} reviews)"

                // Fix: Access favoriteButton through the toolbar
                toolbar.findViewById<ImageButton>(R.id.favoriteButton).setImageResource(
                    if (product.isFavorite) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_outline
                )
            }
        }
    }

    private fun loadSimilarProducts() {
        currentProduct?.let { product ->
            database.getReference("products")
                .orderByChild("category")
                .equalTo(product.category)
                .limitToFirst(5)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val similarProducts = mutableListOf<Product>()
                        snapshot.children.forEach { child ->
                            val productMap = child.value as? Map<String, Any?> ?: return@forEach
                            if (child.key != product.id) {
                                similarProducts.add(Product.fromMap(productMap, child.key ?: ""))
                            }
                        }
                        similarProductsAdapter.updateProducts(similarProducts)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Error loading similar products",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun addToCart() {
        val user = auth.currentUser
        if (user == null) {
            showLoginPrompt()
            return
        }

        currentProduct?.let { product ->
            val cartItemId = UUID.randomUUID().toString()
            val cartItem = CartItem(
                id = cartItemId,
                productId = product.id,
                quantity = currentQuantity,
                timestamp = System.currentTimeMillis()
            )

            database.getReference("users")
                .child(user.uid)
                .child("cart")
                .child(cartItemId)
                .setValue(cartItem.toMap())
                .addOnSuccessListener {
                    showSuccess("${currentProduct?.name ?: ""} Added to cart")
                }
                .addOnFailureListener { e ->
                    showError("Failed to add to cart: ${e.message}")
                }
        }
    }

    private fun toggleFavorite() {
        val user = auth.currentUser
        if (user == null) {
            showLoginPrompt()
            return
        }

        currentProduct?.let { product ->
            if (product.isFavorite) {
                // If it's currently favorited, remove it
                wishlistManager.removeFromWishlist(
                    productId = product.id,
                    onSuccess = {
                        updateFavoriteStatus(false)
                        // Optional: Show feedback
                        showSuccess("Removed from favorites")
                    },
                    onFailure = { e -> showError("Failed to remove from favorites: ${e.message}") }
                )
            } else {
                // If it's not favorited, add it
                wishlistManager.addToWishlist(
                    product = product,
                    onSuccess = {
                        updateFavoriteStatus(true)
                        // Optional: Show feedback
                        showSuccess("Added to favorites")
                    },
                    onFailure = { e -> showError("Failed to add to favorites: ${e.message}") }
                )
            }
        }
    }

    private fun updateFavoriteStatus(isFavorite: Boolean) {
        currentProduct = currentProduct?.copy(isFavorite = isFavorite)
        // Fix: Access favoriteButton through the toolbar
        binding.toolbar.findViewById<ImageButton>(R.id.favoriteButton).setImageResource(
            if (isFavorite) R.drawable.ic_favorite_filled
            else R.drawable.ic_favorite_outline
        )
    }

    private fun shareProduct() {
        currentProduct?.let { product ->
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT, """
                    Check out ${product.name} on our app!
                    Price: $${String.format(resources.configuration.locales[0], "%.2f", product.price)}
                    
                    ${product.description}
                """.trimIndent()
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun showArView(product: Product) {
        if (product.modelUrl.isNotEmpty()) {
            val intent = Intent(this, ARViewerActivity::class.java).apply {
                putExtra("modelUrl", product.modelUrl)
                putExtra("productId", product.id)
            }
            startActivity(intent)

            database.getReference("products")
                .child(product.id)
                .child("lastUsed")
                .setValue(System.currentTimeMillis())
        }
    }

    private fun navigateToProduct(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java)
            .putExtra("product_id", product.id)
        startActivity(intent)
    }

    private fun toggleProductFavorite(product: Product) {
        if (product.isFavorite) {
            wishlistManager.removeFromWishlist(
                product.id,
                onSuccess = {
                    // Handle success
                },
                onFailure = { e ->
                    showError("Failed to remove from favorites: ${e.message}")
                }
            )
        } else {
            wishlistManager.addToWishlist(
                product,
                onSuccess = {
                    // Handle success
                },
                onFailure = { e ->
                    showError("Failed to add to favorites: ${e.message}")
                }
            )
        }
    }

    private fun showLoginPrompt() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign In Required")
            .setMessage("Please sign in to continue")
            .setPositiveButton("Sign In") { _, _ ->
                // Navigate to login
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}