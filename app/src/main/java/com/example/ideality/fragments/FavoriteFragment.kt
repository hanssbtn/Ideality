package com.example.ideality.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ideality.activities.ARViewerActivity
import com.example.ideality.activities.ProductDetailActivity
import com.example.ideality.adapters.ProductAdapter
import com.example.ideality.databinding.FragmentFavoriteBinding
import com.example.ideality.decorations.GridSpacingItemDecoration
import com.example.ideality.managers.WishlistManager
import com.example.ideality.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FavoriteFragment : Fragment() {
    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
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
        setupViews()
        loadFavorites()
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        FirebaseAuth.getInstance().currentUser?.let { user ->
            wishlistManager = WishlistManager(database, user.uid)
        }
    }

    private fun setupViews() {
        // Setup RecyclerView with GridLayoutManager
        productAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { product -> toggleFavorite(product) },
            onQuickArView = { product -> showArView(product) }
        )

        val spacing = (8 * resources.displayMetrics.density).toInt()

        binding.favoritesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
            addItemDecoration(GridSpacingItemDecoration(2, spacing))
            clipToPadding = false
            setPadding(spacing, spacing, spacing, spacing)
        }

        binding.exploreButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun loadFavorites() {
        showLoading(true)

        FirebaseAuth.getInstance().currentUser?.let { user ->
            database.getReference("users")
                .child(user.uid)
                .child("wishlist")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val favoriteProducts = mutableListOf<Product>()
                        var loadedProducts = 0
                        val totalProducts = snapshot.childrenCount.toInt()

                        if (totalProducts == 0) {
                            showEmptyState()
                            return
                        }

                        snapshot.children.forEach { child ->
                            val productId = child.key ?: return@forEach
                            loadProductDetails(productId) { product ->
                                product?.let {
                                    favoriteProducts.add(it.copy(isFavorite = true))
                                }
                                loadedProducts++

                                if (loadedProducts == totalProducts) {
                                    if (favoriteProducts.isEmpty()) {
                                        showEmptyState()
                                    } else {
                                        showProducts(favoriteProducts)
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Error loading favorites: ${error.message}")
                        showEmptyState()
                    }
                })
        } ?: showEmptyState()
    }

    private fun loadProductDetails(productId: String, onComplete: (Product?) -> Unit) {
        database.getReference("products")
            .child(productId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val productMap = snapshot.value as? Map<String, Any?> ?: return
                    val product = Product.fromMap(productMap, snapshot.key ?: "")
                    onComplete(product)
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(null)
                }
            })
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

    private fun showEmptyState() {
        showLoading(false)
        binding.apply {
            emptyStateLayout.visibility = View.VISIBLE
            favoritesRecyclerView.visibility = View.GONE
        }
    }

    private fun showProducts(products: List<Product>) {
        showLoading(false)
        binding.apply {
            emptyStateLayout.visibility = View.GONE
            favoritesRecyclerView.visibility = View.VISIBLE
        }
        productAdapter.updateProducts(products)
    }

    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
        intent.putExtra("product_id", product.id)
        startActivity(intent)
    }

    private fun showArView(product: Product) {
        val intent = Intent(requireContext(), ARViewerActivity::class.java).apply {
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
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
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