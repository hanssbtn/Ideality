package com.example.ideality.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.R
import com.example.ideality.databinding.ActivitySplashScreenBinding

class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private val splashDuration = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system bars (fullscreen)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // Start animations
        startAnimations()

        // Navigate to main activity after splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            startMainActivity()
        }, splashDuration)
    }

    private fun startAnimations() {
        // Set initial alpha to 0
        binding.logoImageView.alpha = 0f
        binding.taglineText.alpha = 0f
        binding.loadingProgress.alpha = 0f

        // Fade in logo
        val logoFadeIn = ObjectAnimator.ofFloat(binding.logoImageView, "alpha", 0f, 1f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Scale up logo
        val logoScaleX = ObjectAnimator.ofFloat(binding.logoImageView, "scaleX", 0.8f, 1f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }
        val logoScaleY = ObjectAnimator.ofFloat(binding.logoImageView, "scaleY", 0.8f, 1f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Fade in tagline
        val taglineFadeIn = ObjectAnimator.ofFloat(binding.taglineText, "alpha", 0f, 1f).apply {
            duration = 800
            startDelay = 500
        }

        // Fade in progress bar
        val progressFadeIn = ObjectAnimator.ofFloat(binding.loadingProgress, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 700
        }

        // Play all animations together
        AnimatorSet().apply {
            playTogether(logoFadeIn, logoScaleX, logoScaleY, taglineFadeIn, progressFadeIn)
            start()
        }
    }

    private fun startMainActivity() {
        // Since this is a login-required app, we'll start with the Login activity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)

        // Add fade transition animation using our custom animations
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        // Close splash screen
        finish()
    }
}