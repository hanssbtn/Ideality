
package com.example.ideality.activities

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.ideality.R
import com.example.ideality.adapter.*
import com.example.ideality.databinding.ActivityHomeBinding
import com.example.ideality.fragments.FavoriteFragment
import com.example.ideality.fragments.ProfileFragment
import com.example.ideality.fragments.TransactionsFragment
import com.example.ideality.managers.WishlistManager
import com.example.ideality.models.Category
import com.example.ideality.models.Product
import com.example.ideality.utils.TestDataUtility
import com.example.ideality.viewmodels.HomeViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.ideality.R as Res
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

    private val homeViewModel: HomeViewModel by viewModels()

    private val autoScrollRunnable = Runnable {
        binding.viewPager.currentItem = (binding.viewPager.currentItem + 1) % carouselImages.size
    }

    private val carouselImages = listOf(
        Res.drawable.samplecarousel1,
        Res.drawable.samplecaraousel2,
        Res.drawable.samplecaraousel3
    )

    private var position = 0

    private val updateVisibility = { position: Int ->
        this@Home.position = position
        when (position) {
            0 -> {
                clearFragments()
                showMainContent(true)
            }
            1 -> { // Favorite tab
                FavoriteFragment.newInstance().let {

                    loadFragment(it)
                }
                showMainContent(false)
            }
            2 -> { // Favorite tab
                val fragment = TransactionsFragment.newInstance()
                loadFragment(fragment)
                showMainContent(false)
            }
            3 -> {
                loadFragment(ProfileFragment())
                showMainContent(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        (this as LifecycleOwner).also {
            homeViewModel.refreshTrigger.observe(this) {
                loadData()
            }
        }

        initializeFirebase()
        setupViews()
        setupManagers()
        loadData()
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
            Category("sofa", "Sofa", Res.drawable.ic_sofa),
            Category("chair", "Chair", Res.drawable.ic_chair),
            Category("table", "Table", Res.drawable.ic_table),
            Category("shelf", "Shelf", Res.drawable.ic_shelf)
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

        binding.seeAllRecent.setOnClickListener {
            val intent = Intent(this, CategoryDetailActivity::class.java).apply {
                putExtra("list_type", "recent")
            }
            startActivity(intent)
        }

        binding.seeAllNew.setOnClickListener {
            val intent = Intent(this, CategoryDetailActivity::class.java).apply {
                putExtra("list_type", "new")
            }
            startActivity(intent)
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
        binding.swipeRefresh.setColorSchemeResources(Res.color.md_theme_onPrimaryFixed, Res.color.md_theme_onPrimaryFixedVariant, Res.color.light_blue)
        binding.swipeRefresh.setOnRefreshListener {
            homeViewModel.trigger()
        }
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
        val intent = Intent(this, CategoryDetailActivity::class.java).apply {
            putExtra("category_id", category.id)
            putExtra("category_name", category.name)
        }
        startActivity(intent)
    }

    private fun showShimmer(show: Boolean) {
        if (show) {
            if (position == 0) {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.shimmerLayout.startShimmer()
            }
            ConstraintSet().let {
                it.clone(binding.containerLayout)
                it.connect(
                    Res.id.newReleasesHeader,
                    ConstraintSet.TOP,
                    Res.id.shimmerLayout,
                    ConstraintSet.BOTTOM,
                    16
                )
                it.applyTo(binding.containerLayout)
            }
            binding.recentlyUsedRecyclerView.visibility = View.GONE
            binding.newReleasesRecyclerView.visibility = View.GONE
        } else {
            if (position == 0) {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
            }
            ConstraintSet().let {
                it.clone(binding.containerLayout)
                it.connect(
                    Res.id.newReleasesHeader,
                    ConstraintSet.TOP,
                    Res.id.recentlyUsedRecyclerView,
                    ConstraintSet.BOTTOM,
                    16
                )
                it.applyTo(binding.containerLayout)
            }
            if (position == 0) {
                binding.recentlyUsedRecyclerView.visibility = View.VISIBLE
                binding.newReleasesRecyclerView.visibility = View.VISIBLE
            }
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

    private fun showArView(product: Product) {
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
                    binding.toolbar.findViewById<ImageButton>(R.id.favoriteButton).setImageResource(
                        if (product.isFavorite) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_outline
                    )
                    Toast.makeText(this, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    binding.toolbar.findViewById<ImageButton>(R.id.favoriteButton).setImageResource(
                        if (product.isFavorite) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_outline
                    )
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            wishlistManager.addToWishlist(
                product,
                onSuccess = {
                    binding.toolbar.findViewById<ImageButton>(R.id.favoriteButton).setImageResource(
                        if (product.isFavorite) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_outline
                    )
                    Toast.makeText(this, "Added to wishlist", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    binding.toolbar.findViewById<ImageButton>(R.id.favoriteButton).setImageResource(
                        if (product.isFavorite) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_outline
                    )
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
        binding.bottomBar.onItemSelected = updateVisibility
    }

    fun showMainContent(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.carouselCardView.visibility = visibility
        binding.viewPager.visibility = visibility
        binding.tabLayout.visibility = visibility
        binding.shimmerLayout.visibility = if (binding.swipeRefresh.isRefreshing) visibility else View.GONE
        binding.recentlyUsedHeader.visibility = visibility
        binding.newReleasesHeader.visibility = visibility
        binding.categoriesRecyclerView.visibility = visibility
        binding.recentlyUsedRecyclerView.visibility = visibility
        binding.newReleasesRecyclerView.visibility = visibility
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(Res.id.fragmentContainer, fragment)
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
