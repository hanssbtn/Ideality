package com.example.ideality.activities

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import com.example.ideality.utils.TestDataUtility
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.ideality.R
import com.example.ideality.adapters.*
import com.example.ideality.databinding.ActivityHomeBinding
import com.example.ideality.fragments.FavoriteFragment
import com.example.ideality.fragments.ProfileFragment
import com.example.ideality.managers.WishlistManager
import com.example.ideality.models.Category
import com.example.ideality.models.Product
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator

class Home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var wishlistManager: WishlistManager
    private lateinit var database: FirebaseDatabase
    private lateinit var productsRef: DatabaseReference

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recentlyUsedAdapter: ProductAdapter
    private lateinit var newReleasesAdapter: ProductAdapter

    private var autoScrollHandler = Handler(Looper.getMainLooper())
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val autoScrollRunnable = Runnable {
        binding.viewPager.currentItem = (binding.viewPager.currentItem + 1) % carouselImages.size
    }

    private val carouselImages = listOf(
        R.drawable.samplecarousel1,
        R.drawable.samplecaraousel2,
        R.drawable.samplecaraousel3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add test product on first launch
        addTestProduct()

        initializeFirebase()
        setupViews()
        setupManagers()
        loadData()
    }

    private fun addTestProduct() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        if (isFirstRun) {
            TestDataUtility.addTestProductToFirebase(this)
            prefs.edit().putBoolean("is_first_run", false).apply()
        }
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        productsRef = database.getReference("products")
    }

    private fun setupViews() {
        setupCarousel()
        setupCategories()
        setupRecyclerViews()
        setupSwipeRefresh()
        setupBottomNavigation()
        setupCartButton()
    }

    private fun setupCartButton() {
        binding.cartLayout.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
    }

    private fun setupCarousel() {
        binding.viewPager.adapter = ImageCarouselAdapter(carouselImages)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
            }
        })
    }

    private fun setupCategories() {
        val categories = listOf(
            Category("sofa", "Sofa", R.drawable.ic_sofa),
            Category("chair", "Chair", R.drawable.ic_chair),
            Category("table", "Table", R.drawable.ic_table),
            Category("shelf", "Shelf", R.drawable.ic_shelf)
        )

        categoryAdapter = CategoryAdapter(categories) { category ->
            filterProductsByCategory(category)
        }

        binding.categoriesRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.HORIZONTAL, false)
            addItemDecoration(createHorizontalSpacing())
        }
    }

    private fun setupRecyclerViews() {
        // Recently Used RecyclerView
        binding.recentlyUsedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = SlideInRightAnimator()
            addItemDecoration(createHorizontalSpacing())
        }

        // New Releases RecyclerView
        binding.newReleasesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = SlideInRightAnimator()
            addItemDecoration(createHorizontalSpacing())
        }

        // Initialize adapters
        recentlyUsedAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { product -> toggleFavorite(product) },
            onQuickArView = { product -> showArView(product) }
        )

        newReleasesAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { product -> toggleFavorite(product) },
            onQuickArView = { product -> showArView(product) }
        )

        binding.recentlyUsedRecyclerView.adapter = recentlyUsedAdapter
        binding.newReleasesRecyclerView.adapter = newReleasesAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.black, R.color.blue, R.color.green)
        binding.swipeRefresh.setOnRefreshListener { loadData() }
    }

    private fun setupManagers() {
        currentUser?.let { user ->
            wishlistManager = WishlistManager(database, user.uid)
        }
    }

    private fun loadData() {
        showShimmer(true)

        productsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allProducts = mutableListOf<Product>()

                snapshot.children.forEach { child ->
                    val productMap = child.value as? Map<String, Any?> ?: return@forEach
                    val product = Product.fromMap(productMap, child.key ?: "")
                    allProducts.add(product)
                }

                // Update recently used products
                val recentlyUsed = allProducts
                    .filter { it.lastUsed != null }
                    .sortedByDescending { it.lastUsed }
                    .take(5)
                recentlyUsedAdapter.updateProducts(recentlyUsed)

                // Update new releases
                val newReleases = allProducts
                    .filter { it.isNew }
                    .take(5)
                newReleasesAdapter.updateProducts(newReleases)

                showShimmer(false)
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Home", "Error loading products: ${error.message}")
                showShimmer(false)
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@Home, "Error loading products", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterProductsByCategory(category: Category) {
        productsRef.orderByChild("category")
            .equalTo(category.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val filteredProducts = mutableListOf<Product>()
                    snapshot.children.forEach { child ->
                        val productMap = child.value as? Map<String, Any?> ?: return@forEach
                        val product = Product.fromMap(productMap, child.key ?: "")
                        filteredProducts.add(product)
                    }
                    // Handle filtered products (e.g., show in a new activity or dialog)
                    Toast.makeText(this@Home, "Found ${filteredProducts.size} ${category.name}s", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Home, "Error filtering products", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showShimmer(show: Boolean) {
        if (show) {
            binding.shimmerLayout.visibility = View.VISIBLE
            binding.shimmerLayout.startShimmer()
            binding.recentlyUsedRecyclerView.visibility = View.GONE
            binding.newReleasesRecyclerView.visibility = View.GONE
        } else {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
            binding.recentlyUsedRecyclerView.visibility = View.VISIBLE
            binding.newReleasesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun createHorizontalSpacing() = object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = if (parent.getChildAdapterPosition(view) == 0) 16 else 8
            outRect.right = 8
        }
    }

    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("product_id", product.id)
        }
        startActivity(intent)
    }

    fun showArView(product: Product) {
        val intent = Intent(this, ARViewerActivity::class.java).apply {
            putExtra("modelUrl", product.modelUrl)
            putExtra("productId", product.id)
        }
        startActivity(intent)

        // Update lastUsed timestamp
        productsRef.child(product.id)
            .child("lastUsed")
            .setValue(System.currentTimeMillis())
    }

    private fun toggleFavorite(product: Product) {
        if (product.isFavorite) {
            wishlistManager.removeFromWishlist(
                product.id,
                onSuccess = {
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
                    Toast.makeText(this, "Added to wishlist", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun updateCartBadge() {
        val userId = currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("cart")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val itemCount = snapshot.childrenCount
                    binding.cartBadge.apply {
                        visibility = if (itemCount > 0) View.VISIBLE else View.GONE
                        text = itemCount.toString()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.cartBadge.visibility = View.GONE
                }
            })
    }


    private fun setupBottomNavigation() {
        binding.bottomBar.onItemSelected = { position ->
            when (position) {
                0 -> {
                    clearFragments()
                    showMainContent(true)
                }
                1 -> { // Favorite tab
                    val fragment = FavoriteFragment.newInstance()
                    loadFragment(fragment)
                    showMainContent(false)
                }
                2 -> Toast.makeText(this, "Transaction", Toast.LENGTH_SHORT).show()
                3 -> {
                    loadFragment(ProfileFragment())
                    showMainContent(false)
                }
            }
        }
    }

    fun showMainContent(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.viewPager.visibility = visibility
        binding.tabLayout.visibility = visibility
        binding.categoriesRecyclerView.visibility = visibility
        binding.recentlyUsedRecyclerView.visibility = visibility
        binding.newReleasesRecyclerView.visibility = visibility
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun clearFragments() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            binding.bottomBar.setActiveItem(0)
            showMainContent(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        binding.shimmerLayout.stopShimmer()
    }



    // Separate function at class level (with other functions)
    private fun refreshCartBadge() {
        if (currentUser != null) {
            updateCartBadge()
        } else {
            binding.cartBadge.visibility = View.GONE
        }
    }



    // Update onResume()
    override fun onResume() {
        super.onResume()
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
        if (binding.shimmerLayout.visibility == View.VISIBLE) {
            binding.shimmerLayout.startShimmer()
        }
        refreshCartBadge() // Add this call
    }
}