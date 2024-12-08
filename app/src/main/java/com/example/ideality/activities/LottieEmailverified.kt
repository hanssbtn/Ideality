package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.ideality.R

class LottieEmailVerifiedActivity : AppCompatActivity() {
    private lateinit var checkAnimation: LottieAnimationView
    val nope = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lottie_emailverified)

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

    @Deprecated("Override for older versions")
    override fun onBackPressed() {
        // Disable back button
        if (!nope) {
            super.onBackPressed()
        }
    }
}

/***
 * <html>'@Deprecated(...) @MainThread() @CallSuper() fun onBackPressed(): Unit' is deprecated.
 * This method has been deprecated in favor of using the<br/>{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.
 * <br/>The OnBackPressedDispatcher controls how back button events are dispatched<br/>to one or more {@link OnBackPressedCallback} objects.
 */