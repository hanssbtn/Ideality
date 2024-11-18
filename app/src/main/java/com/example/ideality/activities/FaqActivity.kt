package com.example.ideality.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideality.R
import com.example.ideality.adapters.FAQAdapter
import com.example.ideality.databinding.ActivityFaqBinding
import com.example.ideality.models.FAQItem
import com.example.ideality.models.FAQSection
import com.google.android.material.snackbar.Snackbar

class FaqActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaqBinding
    private lateinit var faqAdapter: FAQAdapter
    private var allFAQs = mutableListOf<FAQSection>()
    private var filteredFAQs = mutableListOf<FAQSection>()
    private var lastQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupContactSupport()
        loadFAQData()
    }

    private fun setupToolbar() {
        binding.toolbar.findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        faqAdapter = FAQAdapter { section, item, isExpanded ->
            handleFAQClick(section, item, isExpanded)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FaqActivity)
            adapter = faqAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        binding.apply {
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    clearSearchButton.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
                }
                override fun afterTextChanged(s: Editable?) {
                    val query = s?.toString() ?: ""
                    if (query != lastQuery) {
                        lastQuery = query
                        filterFAQs(query)
                    }
                }
            })

            searchEditText.setOnEditorActionListener { textView, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard()
                    filterFAQs(textView.text.toString())
                    true
                } else {
                    false
                }
            }

            clearSearchButton.setOnClickListener {
                searchEditText.text.clear()
                lastQuery = ""
                restoreOriginalList()
                hideKeyboard()
            }
        }
    }

    private fun updateSearchResults() {
        binding.apply {
            if (filteredFAQs.isEmpty()) {
                recyclerView.visibility = View.GONE
                noResultsView.visibility = View.VISIBLE

                noResultsView.findViewById<TextView>(R.id.noResultsText)
                    .text = "No results found for \"$lastQuery\""

                suggestionText.visibility = View.VISIBLE
                suggestionText.text = "Try using different keywords or browse all FAQs"

                popularCategories.visibility = if (lastQuery.isNotEmpty()) View.VISIBLE else View.GONE
            } else {
                recyclerView.visibility = View.VISIBLE
                noResultsView.visibility = View.GONE
                suggestionText.visibility = View.GONE
                popularCategories.visibility = View.GONE

                val totalResults = filteredFAQs.sumBy { it.items.size }
                searchResultCount.apply {
                    visibility = View.VISIBLE
                    text = when (totalResults) {
                        0 -> "No results found"
                        1 -> "1 result found"
                        else -> "$totalResults results found"
                    }
                }
            }

            recyclerView.alpha = 0f
            recyclerView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

            faqAdapter.submitList(filteredFAQs.toList())
        }
    }

    private fun restoreOriginalList() {
        binding.apply {
            searchResultCount.visibility = View.GONE
            noResultsView.visibility = View.GONE
            suggestionText.visibility = View.GONE
            popularCategories.visibility = View.GONE

            filteredFAQs.clear()
            filteredFAQs.addAll(allFAQs)

            recyclerView.visibility = View.VISIBLE
            faqAdapter.submitList(filteredFAQs.toList())

            // Animate the restoration
            recyclerView.alpha = 0f
            recyclerView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun setupContactSupport() {
        binding.contactSupportButton.setOnClickListener {
            if (filteredFAQs.isEmpty() && lastQuery.isNotEmpty()) {
                // If no results found, pass the search query to contact support
                val intent = Intent(this, ContactUsActivity::class.java).apply {
                    putExtra("SEARCH_QUERY", lastQuery)
                }
                startActivity(intent)
            } else {
                startActivity(Intent(this, ContactUsActivity::class.java))
            }
        }
    }

    private fun handleFAQClick(section: FAQSection, item: FAQItem, isExpanded: Boolean) {
        val sectionIndex = filteredFAQs.indexOf(section)
        if (sectionIndex != -1) {
            filteredFAQs[sectionIndex] = section.copy(
                items = section.items.map {
                    if (it == item) it.copy(isExpanded = !isExpanded)
                    else it
                }
            )
            faqAdapter.submitList(filteredFAQs.toList())
        }
    }

    private fun filterFAQs(query: String) {
        if (query.isBlank()) {
            restoreOriginalList()
        } else {
            val searchQuery = query.toLowerCase()
            filteredFAQs.clear()

            allFAQs.forEach { section ->
                val matchingItems = section.items.filter { item ->
                    item.question.toLowerCase().contains(searchQuery) ||
                            item.answer.toLowerCase().contains(searchQuery)
                }

                if (matchingItems.isNotEmpty()) {
                    filteredFAQs.add(section.copy(items = matchingItems))
                }
            }

            updateSearchResults()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    private fun loadFAQData() {
        allFAQs.addAll(listOf(
            createARFeaturesSection(),
            createShoppingSection(),
            createShippingSection(),
            createProductSection(),
            createReturnsSection(),
            createAccountSection(),
            createTechnicalSection(),
            createAppFeaturesSection()
        ))

        filteredFAQs.addAll(allFAQs)
        faqAdapter.submitList(filteredFAQs)
    }

    private fun createARFeaturesSection() = FAQSection(
        "AR Features",
        listOf(
            FAQItem(
                "How do I use the AR feature?",
                "1. Select a product\n2. Tap the AR icon\n3. Point your camera at a flat surface\n4. Move your phone slowly\n5. Tap to place the furniture"
            ),
            FAQItem(
                "What devices support AR view?",
                "AR view is supported on most modern Android devices with ARCore support. Your device must run Android 7.0 or later."
            ),
            FAQItem(
                "Can I save AR snapshots?",
                "Yes! When viewing furniture in AR, tap the camera icon to take a snapshot. Images are saved to your gallery."
            )
        )
    )

    private fun createShoppingSection() = FAQSection(
        "Shopping",
        listOf(
            FAQItem(
                "What payment methods are accepted?",
                "We accept various payment methods including credit/debit cards, bank transfers, and digital wallets."
            ),
            FAQItem(
                "How do I track my order?",
                "Go to Profile > Orders to view your order status and tracking information."
            ),
            FAQItem(
                "Can I cancel my order?",
                "Orders can be cancelled within 24 hours of placement. Contact support for assistance."
            )
        )
    )

    private fun createProductSection() = FAQSection(
        "Product Information",
        listOf(
            FAQItem(
                "How can I check product dimensions?",
                "Product dimensions are listed in the product details section. You can also use the AR feature to visualize the size in your space."
            ),
            FAQItem(
                "Are the colors accurate to the photos?",
                "We strive to display colors as accurately as possible. However, colors may appear slightly different due to screen settings and lighting conditions."
            ),
            FAQItem(
                "What materials are used in your furniture?",
                "Each product listing includes detailed material information. We use high-quality materials and provide care instructions specific to each material type."
            ),
            FAQItem(
                "Do you offer customization options?",
                "Some products offer customization options like color, fabric, or size variations. Look for the 'Customize' button on product pages."
            )
        )
    )

    private fun createShippingSection() = FAQSection(
        "Shipping & Delivery",
        listOf(
            FAQItem(
                "What are your delivery options?",
                "We offer:\n• Standard Delivery (3-5 business days)\n• Express Delivery (1-2 business days)\n• Same-day Delivery (select areas)\n\nDelivery times may vary based on location and product availability."
            ),
            FAQItem(
                "How much does shipping cost?",
                "Shipping costs are calculated based on:\n• Delivery location\n• Item size and weight\n• Chosen delivery method\n\nFree shipping is available for orders over $500."
            ),
            FAQItem(
                "Do you ship internationally?",
                "Currently, we only ship within the United States. We're working on expanding our shipping services to other countries."
            ),
            FAQItem(
                "How can I track my delivery?",
                "Once your order ships, you'll receive:\n• Tracking number via email\n• Real-time updates in the app\n• SMS notifications (if enabled)"
            )
        )
    )

    private fun createReturnsSection() = FAQSection(
        "Returns & Refunds",
        listOf(
            FAQItem(
                "What is your return policy?",
                "Items can be returned within 30 days if:\n• Item is unused\n• In original packaging\n• Has all original tags/labels\n• Includes proof of purchase"
            ),
            FAQItem(
                "How do I start a return?",
                "To initiate a return:\n1. Go to Orders in your profile\n2. Select the item to return\n3. Choose return reason\n4. Print return label or schedule pickup"
            ),
            FAQItem(
                "When will I get my refund?",
                "After we receive and inspect your return:\n• Card refunds: 3-5 business days\n• Store credit: Immediate\n• Bank transfers: 5-7 business days"
            ),
            FAQItem(
                "What items cannot be returned?",
                "Non-returnable items include:\n• Custom/personalized items\n• Clearance items\n• Items marked as final sale\n• Used or assembled furniture"
            )
        )
    )

    private fun createAccountSection() = FAQSection(
        "Account Management",
        listOf(
            FAQItem(
                "How do I reset my password?",
                "To reset your password:\n1. Tap 'Forgot Password' on login screen\n2. Enter your email\n3. Follow reset instructions sent to your email"
            ),
            FAQItem(
                "Can I have multiple addresses?",
                "Yes, you can save multiple shipping addresses in your profile under 'Saved Addresses'."
            ),
            FAQItem(
                "How do I update my profile?",
                "Go to Profile > Edit Profile to update:\n• Personal information\n• Contact details\n• Preferences\n• Notification settings"
            ),
            FAQItem(
                "How do I delete my account?",
                "To delete your account:\n1. Go to Settings\n2. Select 'Account'\n3. Choose 'Delete Account'\n4. Confirm deletion\n\nNote: This action cannot be undone."
            )
        )
    )

    private fun createTechnicalSection() = FAQSection(
        "Technical Support",
        listOf(
            FAQItem(
                "What devices support the AR feature?",
                "Our AR feature works on devices with:\n• Android 7.0 or higher\n• ARCore support\n• At least 2GB RAM\nCheck your device compatibility in Settings > About Phone."
            ),
            FAQItem(
                "How do I update the app?",
                "The app automatically checks for updates. You can also manually update through the Google Play Store."
            ),
            FAQItem(
                "Why isn't AR working on my device?",
                "Ensure your device meets the requirements and has:\n• Adequate lighting\n• Sufficient space\n• Camera permissions enabled\n• Latest ARCore version installed"
            ),
            FAQItem(
                "How can I improve AR performance?",
                "For best AR performance:\n• Ensure good lighting\n• Clear your camera lens\n• Move slowly when scanning\n• Point at textured surfaces\n• Close other apps"
            )
        )
    )

    private fun createAppFeaturesSection() = FAQSection(
        "App Features",
        listOf(
            FAQItem(
                "How do I save items for later?",
                "Tap the heart icon on any product to add it to your wishlist. Access saved items through the Favorites tab."
            ),
            FAQItem(
                "Can I share products with others?",
                "Yes! Use the share button on product pages to share via messaging, email, or social media."
            ),
            FAQItem(
                "How do I track my order in the app?",
                "Go to Profile > Orders to view order status, tracking information, and delivery updates."
            ),
            FAQItem(
                "Can I save my room measurements?",
                "Yes, you can save room measurements in your profile for easier shopping. Go to Profile > Saved Rooms."
            )
        )
    )
}