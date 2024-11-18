package com.example.ideality.activities

import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.facebook.shimmer.ShimmerFrameLayout
import com.example.ideality.R
import com.example.ideality.adapters.*
import com.example.ideality.fragments.ProfileFragment
import com.example.ideality.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.paulrybitskyi.persistentsearchview.PersistentSearchView
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import android.content.Intent
import android.util.Log
import me.ibrahimsn.lib.NiceBottomBar
import com.example.ideality.managers.WishlistManager
import com.example.ideality.managers.SearchHistoryManager
import com.example.ideality.managers.ProductFilterManager

class Home : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var recentlyUsedRecyclerView: RecyclerView
    private lateinit var newReleasesRecyclerView: RecyclerView
    private lateinit var searchView: PersistentSearchView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var bottomBar: NiceBottomBar
    private lateinit var wishlistManager: WishlistManager
    private lateinit var searchHistoryManager: SearchHistoryManager
    private lateinit var productFilterManager: ProductFilterManager

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recentlyUsedAdapter: ProductAdapter
    private lateinit var newReleasesAdapter: ProductAdapter

    private var autoScrollHandler = Handler(Looper.getMainLooper())
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance()

    private val autoScrollRunnable = Runnable {
        viewPager.currentItem = (viewPager.currentItem + 1) % images.size
    }

    private val images = listOf(
        R.drawable.samplecarousel1,
        R.drawable.samplecaraousel2,
        R.drawable.samplecaraousel3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeViews()
        setupManagers()
        setupCarousel()
        setupCategories()
        setupSearch()
        setupRecyclerViews()
        setupSwipeRefresh()
        setupBottomNavigation()
        loadData()
    }

    private fun initializeViews() {
        try {
            viewPager = findViewById(R.id.viewPager)
            tabLayout = findViewById(R.id.tabLayout)
            categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView)
            recentlyUsedRecyclerView = findViewById(R.id.recentlyUsedRecyclerView)
            newReleasesRecyclerView = findViewById(R.id.newReleasesRecyclerView)
            searchView = findViewById(R.id.searchView)
            swipeRefreshLayout = findViewById(R.id.swipeRefresh)
            bottomBar = findViewById(R.id.bottomBar)
            shimmerFrameLayout = findViewById(R.id.shimmerLayout)

            // Verify that required views are not null
            requireNotNull(viewPager) { "ViewPager not found in layout" }
            requireNotNull(tabLayout) { "TabLayout not found in layout" }
            requireNotNull(categoriesRecyclerView) { "Categories RecyclerView not found in layout" }
            requireNotNull(recentlyUsedRecyclerView) { "Recently Used RecyclerView not found in layout" }
            requireNotNull(newReleasesRecyclerView) { "New Releases RecyclerView not found in layout" }
            requireNotNull(searchView) { "SearchView not found in layout" }
            requireNotNull(swipeRefreshLayout) { "SwipeRefreshLayout not found in layout" }
            requireNotNull(bottomBar) { "BottomBar not found in layout" }
        } catch (e: Exception) {
            Log.e("Home", "Error initializing views", e)
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupBottomNavigation() {
        bottomBar.onItemSelected = { position ->
            when (position) {
                0 -> {
                    // Home
                    clearFragments()
                    showMainContent(true)
                }
                1 -> {
                    // Favorite
                    // loadFragment(FavoriteFragment())
                    Toast.makeText(this, "Favorite", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    // Preview
                    // loadFragment(PreviewFragment())
                    Toast.makeText(this, "Preview", Toast.LENGTH_SHORT).show()
                }
                3 -> {
                    // Transaction
                    // loadFragment(TransactionFragment())
                    Toast.makeText(this, "Transaction", Toast.LENGTH_SHORT).show()
                }
                4 -> {
                    // Profile
                    loadFragment(ProfileFragment())
                    showMainContent(false)
                }
            }
        }
    }

    private fun showMainContent(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        viewPager.visibility = visibility
        tabLayout.visibility = visibility
        categoriesRecyclerView.visibility = visibility
        recentlyUsedRecyclerView.visibility = visibility
        newReleasesRecyclerView.visibility = visibility
        searchView.visibility = visibility
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun clearFragments() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            bottomBar.setActiveItem(0)  // This is the correct way to set active item
            showMainContent(true)
        } else {
            super.onBackPressed()
        }
    }


    private fun setupManagers() {
        currentUser?.let { user ->
            wishlistManager = WishlistManager(database, user.uid)
            searchHistoryManager = SearchHistoryManager(this)
            productFilterManager = ProductFilterManager()
        }
    }

    private fun loadRecentlyUsedProducts() {
        // Example data - replace with your actual data loading
        val recentProducts = listOf(
            Product(
                id = "1",
                name = "Modern Sofa",
                description = "Comfortable modern sofa",
                price = 599.99,
                image = R.drawable.placeholder_sofa,
                modelFile = "sofa.glb",
                category = "sofa",
                rating = 4.5f,
                reviewCount = 128,
                isNew = false,
                lastUsed = System.currentTimeMillis()
            )
            // Add more products
        )
        recentlyUsedAdapter.updateProducts(recentProducts)
    }

    private fun loadNewReleases() {
        // Example data - replace with your actual data loading
        val newProducts = listOf(
            Product(
                id = "2",
                name = "Designer Chair",
                description = "Modern designer chair",
                price = 299.99,
                image = R.drawable.placeholder_chair,
                modelFile = "chair.glb",
                category = "chair",
                rating = 4.8f,
                reviewCount = 45,
                isNew = true
            )
            // Add more products
        )
        newReleasesAdapter.updateProducts(newProducts)
    }

    private fun setupSearch() {
        searchView.apply {
            setOnLeftBtnClickListener {
                // Handle back button click
                finish()
            }

            setOnClearInputBtnClickListener {
                // Clear input
                searchView.inputQuery = ""
            }

            setOnSearchConfirmedListener { searchView, query ->
                searchHistoryManager.addSearchQuery(query)
                performSearch(query)
                searchView.collapse()
            }
        }
    }

    private fun updateSuggestions(suggestions: List<String>) {
        // Update search suggestions
        // Implementation depends on your search view library
    }

    private fun setupRecyclerViews() {
        // Setup Recently Used RecyclerView
        recentlyUsedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = SlideInRightAnimator()
            addItemDecoration(createHorizontalSpacing())
        }

        // Setup New Releases RecyclerView
        newReleasesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = SlideInRightAnimator()
            addItemDecoration(createHorizontalSpacing())
        }

        // Initialize adapters
        recentlyUsedAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { product -> toggleFavorite(product) }
        )

        newReleasesAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { product -> toggleFavorite(product) }
        )

        recentlyUsedRecyclerView.adapter = recentlyUsedAdapter
        newReleasesRecyclerView.adapter = newReleasesAdapter
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.black,
            R.color.blue,
            R.color.green
        )

        swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
    }

    private fun loadData() {
        showShimmer(true)

        // Simulated data loading delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Load your actual data here
            loadRecentlyUsedProducts()
            loadNewReleases()
            showShimmer(false)
            swipeRefreshLayout.isRefreshing = false
        }, 1500)
    }

    private fun showShimmer(show: Boolean) {
        if (show) {
            shimmerFrameLayout.visibility = View.VISIBLE
            shimmerFrameLayout.startShimmer()
            recentlyUsedRecyclerView.visibility = View.GONE
            newReleasesRecyclerView.visibility = View.GONE
        } else {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = View.GONE
            recentlyUsedRecyclerView.visibility = View.VISIBLE
            newReleasesRecyclerView.visibility = View.VISIBLE
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

    private fun performSearch(query: String) {
        // Implement your search logic here
        Toast.makeText(this, "Searching for: $query", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToProductDetail(product: Product) {
        // Navigate to product detail screen
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("product_id", product.id)
        }
        startActivity(intent)
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

    private fun showArView(product: Product) {
        // Implement AR view logic
        Toast.makeText(this, "Showing AR view for: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun setupCarousel() {
        // Set up adapter
        viewPager.adapter = ImageCarouselAdapter(images)

        // Connect indicators
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Auto-scroll setup
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Reset auto-scroll timer when page changes
                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
            }
        })
    }

    private fun setupCategories() {
        // Initialize categories
        val categories = listOf(
            Category("sofa", "Sofa", R.drawable.ic_sofa),
            Category("chair", "Chair", R.drawable.ic_chair),
            Category("table", "Table", R.drawable.ic_table),
            Category("shelf", "Shelf", R.drawable.ic_shelf)
        )

        // Setup RecyclerView
        categoryAdapter = CategoryAdapter(categories) { category ->
            // Handle category click
            Toast.makeText(this, "Selected: ${category.name}", Toast.LENGTH_SHORT).show()
            // Add your category click handling here
        }

        categoriesRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.HORIZONTAL, false)
            // Add spacing between items
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.left = if (parent.getChildAdapterPosition(view) == 0) 16 else 8
                    outRect.right = 8
                }
            })
        }
    }

    // Method to update images
    fun updateCarouselImages(newImages: List<Int>) {
        viewPager.adapter = ImageCarouselAdapter(newImages)
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
    }

    override fun onPause() {
        super.onPause()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        shimmerFrameLayout.stopShimmer()
    }

    override fun onResume() {
        super.onResume()
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
        if (shimmerFrameLayout.visibility == View.VISIBLE) {
            shimmerFrameLayout.startShimmer()
        }
    }
}
