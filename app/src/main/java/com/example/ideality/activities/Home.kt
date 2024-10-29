package com.example.ideality.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.ideality.R
import com.example.ideality.adapters.ImageCarouselAdapter

class Home : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout  // Changed variable name
    private var autoScrollHandler = Handler(Looper.getMainLooper())

    private val autoScrollRunnable = Runnable {
        viewPager.currentItem = (viewPager.currentItem + 1) % images.size
    }

    // Easy to update images list
    private val images = listOf(
        R.drawable.samplecarousel1,
        R.drawable.samplecaraousel2,
        R.drawable.samplecaraousel3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)  // Changed to match layout ID

        setupCarousel()
    }

    private fun setupCarousel() {
        // Set up adapter
        viewPager.adapter = ImageCarouselAdapter(images)

        // Connect indicators
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()  // Changed variable name

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

    // Method to update images
    fun updateCarouselImages(newImages: List<Int>) {
        viewPager.adapter = ImageCarouselAdapter(newImages)
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()  // Changed variable name
    }

    override fun onPause() {
        super.onPause()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    override fun onResume() {
        super.onResume()
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
    }
}