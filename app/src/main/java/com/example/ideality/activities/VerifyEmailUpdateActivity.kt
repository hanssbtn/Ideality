package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ideality.databinding.ActivityVerifyEmailUpdateBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VerifyEmailUpdateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyEmailUpdateBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var countDownTimer: CountDownTimer? = null
    private var currentEmail: String = ""
    private var newEmail: String = ""
    private var currentUsername: String = ""
    private var isGoogleUser: Boolean = false
    private var timerSeconds = 30
    private var isProcessing = false
    private var verificationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isProcessing) {
                    showExitDialog()
                }
            }
        })

        initializeFirebase()
        getIntentExtras()
        setupUI()
        setupClickListeners()
        sendVerificationEmail()
        startVerificationCheck()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun getIntentExtras() {
        currentEmail = intent.getStringExtra("email") ?: ""
        newEmail = intent.getStringExtra("newEmail") ?: ""
        currentUsername = intent.getStringExtra("username") ?: ""
        isGoogleUser = intent.getBooleanExtra("isGoogleUser", false)
    }

    private fun setupUI() {
        binding.apply {
            titleText.text = "Verify New Email"
            emailText.text = "We've sent a verification email to your new email address:"
            userEmailText.text = newEmail

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
                // Update and verify new email
                auth.currentUser?.verifyBeforeUpdateEmail(newEmail)?.await()
                startResendTimer()
                showSuccess("Verification email sent")
            } catch (e: Exception) {
                showError("Failed to send verification email: ${e.message}")
            } finally {
                isProcessing = false
                showLoading(false)
            }
        }
    }

    private fun startVerificationCheck() {
        verificationJob = lifecycleScope.launch {
            while (true) {
                try {
                    delay(3000) // Check every 3 seconds
                    auth.currentUser?.reload()?.await()

                    if (auth.currentUser?.email == newEmail && auth.currentUser?.isEmailVerified == true) {
                        // Update final verification status in database
                        val updates = mapOf(
                            "email" to newEmail,
                            "emailVerified" to true,
                            "pendingVerification" to false,
                            "pendingEmail" to null,
                            "username" to if (isGoogleUser) newEmail.substringBefore("@") else currentUsername
                        )

                        val userId = auth.currentUser?.uid ?: throw Exception("User not found")
                        database.getReference("users").child(userId)
                            .updateChildren(updates)
                            .await()

                        // Show success screen
                        showEmailVerifiedScreen()
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
        verificationJob?.cancel()
        countDownTimer?.cancel()

        // Revert changes if not verified
        lifecycleScope.launch {
            try {
                // Revert email in Auth if not verified
                if (auth.currentUser?.isEmailVerified == false) {
                    auth.currentUser?.updateEmail(currentEmail)?.await()
                }

                // Update database
                val userId = auth.currentUser?.uid ?: throw Exception("User not found")
                val updates = mapOf(
                    "email" to currentEmail,
                    "pendingVerification" to false,
                    "username" to currentUsername
                )

                database.getReference("users").child(userId)
                    .updateChildren(updates)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Navigate back
                val intent = Intent(this@VerifyEmailUpdateActivity, EditProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showEmailVerifiedScreen() {
        val intent = Intent(this, EmailVerifiedUpdateActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

    private fun showSuccess(message: String) {
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        verificationJob?.cancel()
        countDownTimer?.cancel()
    }
}