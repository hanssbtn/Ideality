package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ideality.R
import com.example.ideality.adapters.ProductAdapter
import com.example.ideality.models.Product
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.ideality.managers.WishlistManager
import com.example.ideality.managers.CartManager
import com.example.ideality.managers.FirebaseManager
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var ratingCount: TextView
    private lateinit var favoriteButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var shareButton: ImageButton
    private lateinit var arButton: ImageButton
    private lateinit var addToCartButton: MaterialButton
    private lateinit var cartBadge: TextView
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var similarProductsRecyclerView: RecyclerView

    // Quantity selector views
    private lateinit var decreaseButton: ImageButton
    private lateinit var increaseButton: ImageButton
    private lateinit var quantityText: TextView
    private var currentQuantity = 1

    private lateinit var wishlistManager: WishlistManager
    private lateinit var cartManager: CartManager
    private lateinit var similarProductsAdapter: ProductAdapter
    private var product: Product? = null
    private var currentUser = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        initializeViews()
        initializeManagers()
        setupClickListeners()
        setupCartBadge()
        setupQuantitySelector()

        // Get product ID from intent
        val productId = intent.getStringExtra("product_id")
            ?: throw IllegalArgumentException("Product ID must not be null")

        setupSimilarProducts()
        loadProduct(productId)
    }

    private fun initializeViews() {
        productImage = findViewById(R.id.productImage)
        productName = findViewById(R.id.productName)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)
        ratingBar = findViewById(R.id.ratingBar)
        ratingCount = findViewById(R.id.ratingCount)
        favoriteButton = findViewById(R.id.favoriteButton)
        backButton = findViewById(R.id.backButton)
        shareButton = findViewById(R.id.shareButton)
        arButton = findViewById(R.id.arButton)
        addToCartButton = findViewById(R.id.addToCartButton)
        cartBadge = findViewById(R.id.cartBadge)
        similarProductsRecyclerView = findViewById(R.id.similarProductsRecyclerView)

        // Initialize quantity selector views
        decreaseButton = findViewById(R.id.decreaseButton)
        increaseButton = findViewById(R.id.increaseButton)
        quantityText = findViewById(R.id.quantityText)
    }

    private fun initializeManagers() {
        wishlistManager = WishlistManager(database, currentUser?.uid ?: "")
        cartManager = CartManager(database)
        firebaseManager = FirebaseManager() // Add this line
    }

    private fun setupQuantitySelector() {
        updateQuantityUI()

        decreaseButton.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                updateQuantityUI()
            }
        }

        increaseButton.setOnClickListener {
            if (currentQuantity < 99) {
                currentQuantity++
                updateQuantityUI()
            }
        }
    }

    private fun updateQuantityUI() {
        quantityText.text = currentQuantity.toString()

        // Update decrease button state
        decreaseButton.isEnabled = currentQuantity > 1
        decreaseButton.alpha = if (currentQuantity > 1) 1f else 0.5f

        // Update increase button state
        increaseButton.isEnabled = currentQuantity < 99
        increaseButton.alpha = if (currentQuantity < 99) 1f else 0.5f
    }

    private fun setupCartBadge() {
        cartManager.observeCartItems(
            onItemsChanged = { items ->
                val totalItems = items.sumOf { it.quantity }
                if (totalItems > 0) {
                    cartBadge.visibility = View.VISIBLE
                    cartBadge.text = totalItems.toString()
                } else {
                    cartBadge.visibility = View.GONE
                }
            },
            onError = { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupSimilarProducts() {
        similarProductsAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProduct(product) },
            onFavoriteClick = { product -> toggleFavorite(product) }
        )

        similarProductsRecyclerView.apply {
            adapter = similarProductsAdapter
            layoutManager = LinearLayoutManager(
                this@ProductDetailActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener { onBackPressed() }

        arButton.setOnClickListener {
            launchArView()
        }

        addToCartButton.setOnClickListener {
            addToCart()
        }

        shareButton.setOnClickListener {
            shareProduct()
        }

        favoriteButton.setOnClickListener {
            product?.let { toggleFavorite(it) }
        }

        findViewById<View>(R.id.cartLayout)?.setOnClickListener {
            // Navigate to cart activity
            // startActivity(Intent(this, CartActivity::class.java))
        }


    }

    private fun loadProduct(productId: String) {
        // Temporary solution until Firebase is set up
        product = Product(
            id = productId,
            name = "Modern Sofa",
            description = "A comfortable modern sofa perfect for your living room...",
            price = 599.99,
            image = R.drawable.placeholder_sofa,
            modelFile = "sofa.glb",
            category = "sofa",
            rating = 4.5f,
            reviewCount = 128,
            isNew = false
        )

        updateUI()
        loadSimilarProducts() // This will load dummy similar products
    }

    /*private fun loadProduct(productId: String) {
        firebaseManager.getProduct(
            productId,
            onSuccess = { product ->
                if (product != null) {
                    this.product = product
                    updateUI()

                    // Load similar products
                    firebaseManager.getSimilarProducts(
                        category = product.category,
                        excludeProductId = product.id,
                        onSuccess = { similarProducts ->
                            similarProductsAdapter.updateProducts(similarProducts)
                        },
                        onFailure = { e ->
                            Toast.makeText(this, "Error loading similar products: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            onFailure = { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }*/

    private fun updateUI() {
        product?.let { product ->
            productName.text = product.name
            productPrice.text = NumberFormat.getCurrencyInstance(Locale.US).format(product.price)
            productDescription.text = product.description
            ratingBar.rating = product.rating
            ratingCount.text = "(${product.reviewCount} reviews)"
            productImage.setImageResource(product.image)
            updateFavoriteButton(product.isFavorite)
        }
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite
        )
    }

    private fun launchArView() {
        product?.let { product ->
            Toast.makeText(this, "Launching AR view for ${product.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToCart() {
        product?.let { product ->
            cartManager.addToCart(
                product = product,
                quantity = currentQuantity,
                onSuccess = {
                    Toast.makeText(this, "${currentQuantity}x ${product.name} added to cart", Toast.LENGTH_SHORT).show()
                    addToCartButton.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.scale_click)
                    )
                    // Reset quantity after adding to cart
                    currentQuantity = 1
                    updateQuantityUI()
                },
                onFailure = { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun shareProduct() {
        product?.let { product ->
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this ${product.name} on Ideality!")
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun toggleFavorite(product: Product) {
        currentUser?.let { user ->
            if (product.isFavorite) {
                wishlistManager.removeFromWishlist(
                    product.id,
                    onSuccess = {
                        updateFavoriteButton(false)
                        Toast.makeText(this, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                wishlistManager.addToWishlist(
                    product,
                    onSuccess = {
                        updateFavoriteButton(true)
                        Toast.makeText(this, "Added to wishlist", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun loadSimilarProducts() {
        val similarProducts = listOf(
            Product(
                id = "2",
                name = "Modern Chair",
                description = "Comfortable chair...",
                price = 299.99,
                image = R.drawable.placeholder_chair,
                modelFile = "chair.glb",
                category = "chair",
                rating = 4.2f,
                reviewCount = 85,
                isNew = true
            )
        )
        similarProductsAdapter.updateProducts(similarProducts)
    }

    private fun navigateToProduct(product: Product) {
        if (product.id == this.product?.id) return

        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("product_id", product.id)
        }
        startActivity(intent)
    }
}