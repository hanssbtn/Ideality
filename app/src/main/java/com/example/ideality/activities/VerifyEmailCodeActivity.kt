package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.os.VibratorManager
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AnimationUtils
import com.example.ideality.R
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.ideality.databinding.ActivityVerifyEmailCodeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Random
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VerifyEmailCodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyEmailCodeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var countDownTimer: CountDownTimer? = null
    private var verificationCode: String = ""
    private var newEmail: String = ""
    private var currentUsername: String = ""
    private var isGoogleUser: Boolean = false
    private var timerSeconds = 30
    private var isProcessing = false
    private lateinit var emailSender: EmailSender
    private val digitInputs = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        initializeEmailSender()
        setupUI()
        setupDigitInputs()
        setupClickListeners()
        generateAndSendCode()
    }

    private fun initializeEmailSender() {
        emailSender = EmailSender(
            username = "mhemail12345@gmail.com", // Replace with your email
            password = "nujy jvge xwyp cotm"     // Replace with your app password
        )
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun setupUI() {
        // Get data from intent
        newEmail = intent.getStringExtra("email") ?: ""
        currentUsername = intent.getStringExtra("username") ?: ""
        isGoogleUser = intent.getBooleanExtra("isGoogleUser", false)

        binding.apply {
            userEmailText.text = newEmail

            // Initialize digit inputs
            digitInputs.addAll(listOf(digit1, digit2, digit3, digit4, digit5, digit6))

            verifyButton.isEnabled = false
            progressBar.visibility = View.GONE
        }

        startResendTimer()
    }

    private fun setupDigitInputs() {
        digitInputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Move to next digit
                    if (s?.length == 1) {
                        if (index < digitInputs.size - 1) {
                            digitInputs[index + 1].requestFocus()
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    validateCode()
                }
            })

            // Handle backspace
            editText.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && editText.text.isEmpty() && index > 0) {
                    digitInputs[index - 1].apply {
                        requestFocus()
                        setText("")
                    }
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            backButton.setOnClickListener {
                if (!isProcessing) {
                    showExitDialog()
                }
            }

            verifyButton.setOnClickListener {
                if (!isProcessing) {
                    verifyCode()
                }
            }

            resendButton.setOnClickListener {
                if (!isProcessing && timerSeconds <= 0) {
                    generateAndSendCode()
                }
            }
        }
    }

    private fun generateAndSendCode() {
        isProcessing = true
        showLoading(true)

        // Generate 6-digit code
        verificationCode = Random().nextInt(999999).toString().padStart(6, '0')

        lifecycleScope.launch {
            try {
                // Send email with code
                withContext(Dispatchers.IO) {
                    try {
                        emailSender.sendEmail(
                            toEmail = newEmail,
                            subject = "Email Verification Code",
                            body = """
                            Your verification code is: $verificationCode
                            
                            This code will expire in 5 minutes.
                            If you didn't request this code, please ignore this email.
                            
                            Best regards,
                            Your App Team
                        """.trimIndent()
                        )
                        withContext(Dispatchers.Main) {
                            showSuccess("Verification code sent to your email")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showError("Failed to send email: ${e.localizedMessage}")
                        }
                        e.printStackTrace()
                    }
                }
                startResendTimer()
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Failed to send verification code")
            } finally {
                isProcessing = false
                showLoading(false)
            }
        }
    }

    private fun validateCode() {
        val enteredCode = digitInputs.joinToString("") { it.text.toString() }
        binding.verifyButton.isEnabled = enteredCode.length == 6
        binding.errorText.visibility = View.GONE
    }


    private fun verifyCode() {
        val enteredCode = digitInputs.joinToString("") { it.text.toString() }

        if (enteredCode == verificationCode) {
            updateEmail()
        } else {
            showInvalidCodeError()
        }
    }

    private fun showInvalidCodeError() {
        binding.apply {
            // Show error text
            errorText.text = "Invalid code. Please try again."
            errorText.visibility = View.VISIBLE

            // Shake animation
            val shake = AnimationUtils.loadAnimation(this@VerifyEmailCodeActivity, R.anim.shake)
            codeInputLayout.startAnimation(shake)

            // Clear inputs
            digitInputs.forEach { it.setText("") }
            digitInputs[0].requestFocus()

            // Vibrate for error feedback
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    private fun updateEmail() {
        isProcessing = true
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Get current user and credential
                val user = auth.currentUser ?: throw Exception("User not found")
                val isGoogleUser = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

                if (isGoogleUser) {
                    // For Google users, we need to re-authenticate with Google
                    // Show Google sign in again
                    showError("Please re-authenticate with Google first")
                    navigateToReAuth()
                    return@launch
                } else {
                    // For email users, prompt for password
                    showPasswordReAuthDialog()
                }
            } catch (e: Exception) {
                showError("Failed to update email: ${e.message}")
                isProcessing = false
                showLoading(false)
            }
        }
    }

    private fun showPasswordReAuthDialog() {
        if (isFinishing) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_password_confirm, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Verify Identity")
            .setMessage("Please enter your current password to update email")
            .setView(dialogView)
            .setPositiveButton("Confirm", null)
            .setNegativeButton("Cancel") { _, _ ->
                showLoading(false)
                isProcessing = false
            }
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener { dialog ->
                    getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val password = passwordInput.text.toString()
                        if (password.isEmpty()) {
                            passwordInput.error = "Password is required"
                            return@setOnClickListener
                        }
                        dialog.dismiss()
                        completeEmailUpdate(password)
                    }
                }
                show()
            }
    }

    private fun completeEmailUpdate(password: String) {
        lifecycleScope.launch {
            try {
                val user = auth.currentUser ?: throw Exception("User not found")

                // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(user.email ?: "", password)
                user.reauthenticate(credential).await()

                // Update email in Auth
                user.updateEmail(newEmail).await()

                // Update database
                val userId = user.uid
                val updates = mutableMapOf<String, Any>(
                    "email" to newEmail,
                    "emailVerified" to true
                )

                // Update username if Google user
                if (isGoogleUser) {
                    val googleUsername = newEmail.substringBefore("@")
                    updates["username"] = googleUsername
                } else {
                    updates["username"] = currentUsername
                }

                // Update database
                database.getReference("users").child(userId)
                    .updateChildren(updates)
                    .await()

                // Show success screen
                showEmailVerifiedScreen()
            } catch (e: Exception) {
                showError("Failed to update email: ${e.message}")
                isProcessing = false
                showLoading(false)
            }
        }
    }


    private fun navigateToReAuth() {
        // Navigate back to EditEmailActivity for re-authentication
        val intent = Intent(this, EditEmailActivity::class.java)
        intent.putExtra("requireReAuth", true)
        intent.putExtra("newEmail", newEmail)
        startActivity(intent)
        finish()
    }

    // Email sender class
    private class EmailSender(private val username: String, private val password: String) {
        fun sendEmail(toEmail: String, subject: String, body: String) {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
            }

            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication() = javax.mail.PasswordAuthentication(username, password)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject)
                setText(body)
            }

            Transport.send(message)
        }
    }


    private fun showEmailVerifiedScreen() {
        val intent = Intent(this, EmailVerifiedUpdateActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startResendTimer() {
        binding.resendButton.isEnabled = false
        binding.resendButton.alpha = 0.5f

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerSeconds = (millisUntilFinished / 1000).toInt()
                binding.timerText.text = "Resend code in: ${timerSeconds}s"
                binding.timerText.visibility = View.VISIBLE
            }

            override fun onFinish() {
                timerSeconds = 0
                binding.timerText.visibility = View.GONE
                binding.resendButton.isEnabled = true
                binding.resendButton.alpha = 1.0f
            }
        }.start()
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
        countDownTimer?.cancel()
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            verifyButton.isEnabled = !show
            digitInputs.forEach { it.isEnabled = !show }
            backButton.isEnabled = !show
            resendButton.isEnabled = !show && timerSeconds <= 0
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

    override fun onBackPressed() {
        super.onBackPressed()
        if (!isProcessing) {
            showExitDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}