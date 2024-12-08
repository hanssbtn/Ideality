package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class VerifyEmailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userEmail: String
    private lateinit var backButton: ImageView
    private lateinit var emailText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var openEmailButton: MaterialButton
    private lateinit var timerText: TextView
    private lateinit var tryAnotherEmail: TextView
    private var verificationTimer: CountDownTimer? = null
    private var isCheckingVerification = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        auth = FirebaseAuth.getInstance()
        userEmail = intent.getStringExtra("email") ?: ""

        initializeViews()
        setupClickListeners()
        startVerificationCheck()
        startResendTimer()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        emailText = findViewById(R.id.emailText)
        userEmailText = findViewById(R.id.userEmailText)
        openEmailButton = findViewById(R.id.openEmailButton)
        timerText = findViewById(R.id.timerText)
        tryAnotherEmail = findViewById(R.id.tryAnotherEmail)

        userEmailText.text = userEmail
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        openEmailButton.setOnClickListener {
            openEmailApp()
        }

        tryAnotherEmail.setOnClickListener {
            // Return to signup with clear backstack
            val intent = Intent(this, SignUp::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun startVerificationCheck() {
        if (isCheckingVerification) return

        isCheckingVerification = true
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user?.isEmailVerified == true) {
                            isCheckingVerification = false
                            // Sign out the user first before showing success
                            auth.signOut()
                            showVerificationSuccess()
                        } else {
                            handler.postDelayed(this, 3000) // Check every 3 seconds
                        }
                    }
                }
            }
        }
        handler.post(runnable)
    }

    private fun startResendTimer() {
        verificationTimer?.cancel()
        verificationTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerText.text = "Resend link in: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                timerText.text = "Resend verification link"
                timerText.setOnClickListener {
                    auth.currentUser?.sendEmailVerification()
                    startResendTimer()
                }
            }
        }.start()
    }

    private fun openEmailApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            showError("No email app found")
        }
    }

    private fun showVerificationSuccess() {
        // Show success animation
        startActivity(Intent(this, LottieEmailVerifiedActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        verificationTimer?.cancel()
    }
}