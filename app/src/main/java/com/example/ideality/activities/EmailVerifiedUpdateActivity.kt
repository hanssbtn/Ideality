package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.databinding.ActivityLottieEmailUpdatedBinding

class EmailVerifiedUpdateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLottieEmailUpdatedBinding
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLottieEmailUpdatedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimation()
    }

    private fun setupAnimation() {
        // Configure and start animation
        binding.checkAnimation.apply {
            speed = 0.8f // Slightly slower for better visibility
            playAnimation()
        }

        // Navigate after animation
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isNavigating) {
                isNavigating = true
                navigateToProfile()
            }
        }, 2000) // 2 seconds delay
    }

    private fun navigateToProfile() {
        val intent = Intent(this, EditProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Prevent back navigation during animation
        if (!isNavigating) {
            isNavigating = true
            navigateToProfile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.checkAnimation.cancelAnimation()
    }
}