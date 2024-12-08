package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.ideality.R

class LottieEmailVerifiedActivity : AppCompatActivity() {
    private lateinit var checkAnimation: LottieAnimationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Newer method for disabling back presses
        setContentView(R.layout.activity_lottie_emailverified)
        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {// true: for prevent back and do something in handleOnBackPressed
                override fun handleOnBackPressed() {
                    Toast.makeText(this@LottieEmailVerifiedActivity, "Back button is disabled.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        checkAnimation = findViewById(R.id.checkAnimation)

        // Play animation
        checkAnimation.playAnimation()

        // Auto-navigate to login after animation
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 2000) // Show animation for 2 seconds


    }

}