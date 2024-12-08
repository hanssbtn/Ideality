package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ideality.databinding.ActivityVerifyCurrentEmailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VerifyCurrentEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyCurrentEmailBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var countDownTimer: CountDownTimer? = null
    private var currentEmail: String = ""
    private var timerSeconds = 30
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyCurrentEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        getIntentExtras()
        setupUI()
        setupClickListeners()
        // Check intent right away
        intent?.let { handleIntent(it) }
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun getIntentExtras() {
        currentEmail = intent.getStringExtra("currentEmail") ?: ""
    }

    private fun setupUI() {
        binding.apply {
            titleText.text = "Verify Current Email"
            emailText.text = "We've sent a verification link to your current email address:"
            userEmailText.text = currentEmail
            infoText.text = "Please verify your current email before proceeding with the email change."

            progressBar.visibility = View.GONE
            openEmailButton.isEnabled = true
            resendButton.isEnabled = false
        }

        startResendTimer()
    }

    private fun setupClickListeners() {
        binding.apply {
            backButton.setOnClickListener {
                if (!isProcessing) {
                    showExitDialog()
                }
            }

            openEmailButton.setOnClickListener {
                if (!isProcessing) {
                    openEmailApp()
                }
            }

            resendButton.setOnClickListener {
                if (!isProcessing && timerSeconds <= 0) {
                    sendVerificationEmail()
                }
            }
        }
    }

    private fun sendVerificationEmail() {
        if (isProcessing) return
        isProcessing = true
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Send verification email to current email
                auth.currentUser?.sendEmailVerification()?.await()
                startResendTimer()
                showSuccess()
            } catch (e: Exception) {
                showError("Failed to send verification email: ${e.message}")
            } finally {
                isProcessing = false
                showLoading(false)
            }
        }
    }

    private fun startResendTimer() {
        binding.resendButton.isEnabled = false
        binding.resendButton.alpha = 0.5f

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isFinishing) {
                    timerSeconds = (millisUntilFinished / 1000).toInt()
                    binding.timerText.text = "Resend email in: ${timerSeconds}s"
                    binding.timerText.visibility = View.VISIBLE
                }
            }

            override fun onFinish() {
                if (!isFinishing) {
                    timerSeconds = 0
                    binding.timerText.visibility = View.GONE
                    binding.resendButton.isEnabled = true
                    binding.resendButton.alpha = 1.0f
                }
            }
        }.start()
    }

    private fun openEmailApp() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            showError("No email app found")
        }
    }

    // Check for email link in onNewIntent and onCreate
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent) {
        val emailLink = intent.data?.toString()
        if (emailLink != null && auth.isSignInWithEmailLink(emailLink)) {
            verifyEmailLink(emailLink)
        }
    }

    private fun checkForEmailLink(intent: Intent?) {
        val emailLink = intent?.data?.toString()
        if (emailLink != null && auth.isSignInWithEmailLink(emailLink)) {
            verifyEmailLink(emailLink)
        }
    }

    private fun verifyEmailLink(emailLink: String) {
        isProcessing = true
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Create credential and re-authenticate
                val credential = EmailAuthProvider.getCredentialWithLink(currentEmail, emailLink)
                auth.currentUser?.reauthenticate(credential)?.await()

                // Get saved new email info
                val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
                val newEmail = sharedPrefs.getString("pendingNewEmail", null)
                val currentUsername = sharedPrefs.getString("currentUsername", null)
                val isGoogleUser = sharedPrefs.getBoolean("isGoogleUser", false)

                if (newEmail != null && currentUsername != null) {
                    // Clear saved data
                    sharedPrefs.edit().clear().apply()

                    // Proceed with new email verification
                    val intent = Intent(this@VerifyCurrentEmailActivity, VerifyEmailUpdateActivity::class.java)
                    intent.putExtra("email", currentEmail)
                    intent.putExtra("newEmail", newEmail)
                    intent.putExtra("username", currentUsername)
                    intent.putExtra("isGoogleUser", isGoogleUser)
                    startActivity(intent)
                    finish()
                } else {
                    showError("Missing email information")
                    navigateBack()
                }
            } catch (e: Exception) {
                showError("Failed to verify email: ${e.message}")
                showLoading(false)
                isProcessing = false
            }
        }
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Email Change")
            .setMessage("Are you sure you want to cancel the email verification process?")
            .setPositiveButton("Yes") { _, _ ->
                navigateBack()
            }
            .setNegativeButton("No", null)
            .setCancelable(false)
            .show()
    }

    private fun navigateBack() {
        // Clear saved data
        getSharedPreferences("auth", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Navigate back to profile
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            openEmailButton.isEnabled = !show
            resendButton.isEnabled = !show && timerSeconds <= 0
            backButton.isEnabled = !show
        }
    }

    private fun showError(message: String) {
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccess() {
        if (!isFinishing) {
            Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (!isProcessing) {
            showExitDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}