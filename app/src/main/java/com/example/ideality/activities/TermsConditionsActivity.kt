package com.example.ideality.activities

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import com.example.ideality.R
import com.example.ideality.databinding.ActivityTermsConditionsBinding
import com.google.android.material.card.MaterialCardView

class TermsConditionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTermsConditionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStatusBar()
        setupToolbar()
        setupScrollBehavior()
        setupAnimations()
        setupCardInteractions()
    }

    private fun setupStatusBar() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        var isToolbarElevated = false
        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                if (scrollY > 0 && !isToolbarElevated) {
                    binding.toolbar.elevation = resources.getDimension(R.dimen.toolbar_elevation)
                    binding.divider.elevation = resources.getDimension(R.dimen.toolbar_elevation)
                    isToolbarElevated = true
                } else if (scrollY == 0 && isToolbarElevated) {
                    binding.toolbar.elevation = 0f
                    binding.divider.elevation = 0f
                    isToolbarElevated = false
                }
            }
        )
    }

    private fun setupScrollBehavior() {
        binding.nestedScrollView.isNestedScrollingEnabled = true
    }

    private fun setupAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)

        // Define all sections to animate
        val sections = listOf(
            binding.versionCard,
            binding.agreementSection,
            binding.arUsageSection,
            binding.purchaseTermsSection,
            binding.returnsSection,
            binding.userAccountSection,
            binding.disclaimerCard,
            binding.contactCard
        )

        // Animate each section with a delay
        sections.forEachIndexed { index, section ->
            section.alpha = 0f
            section.postDelayed({
                section.alpha = 1f
                section.startAnimation(fadeIn)
                section.startAnimation(slideDown)
            }, index * 100L)
        }
    }

    private fun setupCardInteractions() {
        val cardElevation = AnimationUtils.loadAnimation(this, R.anim.card_elevation)
        val cardElevationReset = AnimationUtils.loadAnimation(this, R.anim.card_elevation_reset)

        // Find all MaterialCardView instances
        val cards = findAllCards(binding.root)

        // Add touch animation to each card
        cards.forEach { card ->
            card.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.startAnimation(cardElevation)
                        view.animate()
                            .scaleX(0.98f)
                            .scaleY(0.98f)
                            .setDuration(200)
                            .start()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.startAnimation(cardElevationReset)
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun findAllCards(view: View): List<MaterialCardView> {
        val cards = mutableListOf<MaterialCardView>()
        if (view is MaterialCardView) {
            cards.add(view)
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                cards.addAll(findAllCards(view.getChildAt(i)))
            }
        }
        return cards
    }

    companion object {
        private const val TAG = "TermsConditionsActivity"
    }
}