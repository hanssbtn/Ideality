package com.example.ideality.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideality.R
import com.example.ideality.activities.Home
import com.example.ideality.activities.ProductDetailActivity
import com.example.ideality.adapters.ProductAdapter
import com.example.ideality.databinding.FragmentFavoriteBinding
import com.example.ideality.managers.WishlistManager
import com.example.ideality.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoriteFragment : Fragment() {
    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var wishlistManager: WishlistManager
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFirebase()
        setupRecyclerView()
        setupButtons()
        loadFavorites()

        (requireActivity() as? Home)?.let { homeActivity ->
            homeActivity.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        }
    }



    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { user ->
            wishlistManager = WishlistManager(database, user.uid)
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { product -> toggleFavorite(product) },
            onQuickArView = { product -> showArView(product) }
        )

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupButtons() {
        binding.exploreButton.setOnClickListener {
            // Navigate back to home
            (activity as? Home)?.let {
                it.clearFragments()
                it.showMainContent(true)
            }
        }
    }

    private fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return
        showLoading(true)

        database.getReference("users")
            .child(userId)
            .child("wishlist")
            .addValueEventListener(object : ValueEventListener {  // Use ValueEventListener instead of SingleValueEvent
                override fun onDataChange(snapshot: DataSnapshot) {
                    val favoriteIds = snapshot.children.mapNotNull { it.key }
                    if (favoriteIds.isEmpty()) {
                        showEmptyState()
                        showLoading(false)
                        return
                    }
                    loadFavoriteProducts(favoriteIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Error loading favorites: ${error.message}")
                    showLoading(false)
                }
            })
    }

    fun refreshFavorites() {
        loadFavorites()
    }

    private fun loadFavoriteProducts(favoriteIds: List<String>) {
        val favorites = mutableListOf<Product>()
        var loadedCount = 0

        favoriteIds.forEach { productId ->
            database.getReference("products")
                .child(productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val productMap = snapshot.value as? Map<String, Any?> ?: return
                        val product = Product.fromMap(productMap, snapshot.key ?: "")
                            .copy(isFavorite = true)  // Make sure to set isFavorite to true
                        favorites.add(product)

                        loadedCount++
                        if (loadedCount == favoriteIds.size) {
                            updateUI(favorites.sortedByDescending { it.lastUsed })  // Sort by recently used
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == favoriteIds.size) {
                            updateUI(favorites.sortedByDescending { it.lastUsed })
                        }
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites() // Refresh when fragment resumes
    }

    private fun updateUI(products: List<Product>) {
        showLoading(false)
        if (products.isEmpty()) {
            showEmptyState()
        } else {
            showProducts(products)
        }
    }

    private fun showEmptyState() {
        binding.apply {
            emptyStateLayout.visibility = View.VISIBLE
            favoritesRecyclerView.visibility = View.GONE
            shimmerLayout.visibility = View.GONE
        }
    }

    private fun showProducts(products: List<Product>) {
        binding.apply {
            emptyStateLayout.visibility = View.GONE
            favoritesRecyclerView.visibility = View.VISIBLE
            shimmerLayout.visibility = View.GONE
        }
        productAdapter.updateProducts(products)
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            shimmerLayout.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                shimmerLayout.startShimmer()
                favoritesRecyclerView.visibility = View.GONE
                emptyStateLayout.visibility = View.GONE
            } else {
                shimmerLayout.stopShimmer()
            }
        }
    }

    private fun toggleFavorite(product: Product) {
        if (product.isFavorite) {
            wishlistManager.removeFromWishlist(
                product.id,
                onSuccess = {
                    // Product will be removed through the ValueEventListener
                },
                onFailure = { e ->
                    showError("Failed to remove from favorites: ${e.message}")
                }
            )
        }
    }

    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
            putExtra("product_id", product.id)
        }
        startActivity(intent)
    }

    private fun showArView(product: Product) {
        (activity as? Home)?.showArView(product)
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FavoriteFragment()
    }
}