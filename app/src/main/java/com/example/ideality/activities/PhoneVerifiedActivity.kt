package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.databinding.ActivityPhoneVerifiedBinding

class PhoneVerifiedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneVerifiedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneVerifiedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Automatically navigate back after animation
        binding.checkAnimation.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Add a small delay after animation ends before navigating back
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToProfile()
                }, 1000) // 1 second delay
            }
        })
    }

    private fun navigateToProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    // Prevent back button from interrupting success screen
    @Deprecated("Compatibility purposes only.")
    override fun onBackPressed() {
        @Suppress("Deprecation")
        if (false) super.onBackPressed()
        // Do nothing - let the animation complete
    }
}