package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ideality.R
import com.example.ideality.databinding.ActivityVerifyPhoneBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class VerifyPhoneActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyPhoneBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var phoneNumber: String
    private lateinit var verificationId: String
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countDownTimer: CountDownTimer? = null
    private var currentUsername: String = ""
    private var timerSeconds = 60
    private var isProcessing = false
    private val digitInputs = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        getIntentExtras()
        setupUI()
        setupClickListeners()
        setupCodeInputs()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Add this for testing
        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
    }

    private fun getIntentExtras() {
        verificationId = intent.getStringExtra("verificationId") ?: ""
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        currentUsername = intent.getStringExtra("username") ?: ""
    }

    private fun setupUI() {
        binding.apply {
            userPhoneText.text = phoneNumber
            digitInputs.addAll(listOf(digit1, digit2, digit3, digit4, digit5, digit6))

            // Setup error text
            errorText.apply {
                visibility = View.GONE
                setTextColor(ContextCompat.getColor(this@VerifyPhoneActivity, R.color.error_red))
                textSize = 14f
            }

            // Setup note text
            noteText.visibility = View.VISIBLE

            verifyButton.isEnabled = false
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

            verifyButton.setOnClickListener {
                if (!isProcessing) {
                    val code = getEnteredCode()
                    verifyCode(code)
                }
            }

            resendButton.setOnClickListener {
                if (!isProcessing && timerSeconds <= 0) {
                    resendVerificationCode()
                }
            }
        }
    }

    private fun setupCodeInputs() {
        digitInputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // When user starts typing, hide error and show note
                    binding.apply {
                        errorText.visibility = View.GONE
                        noteText.visibility = View.VISIBLE
                    }

                    // Auto-advance to next digit
                    if (s?.length == 1 && index < digitInputs.size - 1) {
                        digitInputs[index + 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    // Enable verify button if all digits are filled
                    binding.verifyButton.isEnabled = getEnteredCode().length == 6
                }
            })


            // Handle backspace for previous digit
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    editText.text.isEmpty() &&
                    index > 0 &&
                    event.action == KeyEvent.ACTION_DOWN) {
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

    private fun getEnteredCode(): String {
        return digitInputs.joinToString("") { it.text.toString() }
    }

    private fun verifyCode(code: String) {
        if (isProcessing) return

        if (code.length != 6) {
            binding.errorText.apply {
                text = "Please enter all 6 digits"
                visibility = View.VISIBLE
            }
            val shake = AnimationUtils.loadAnimation(this@VerifyPhoneActivity, R.anim.shake)
            binding.codeInputLayout.startAnimation(shake)
            return
        }

        isProcessing = true
        showLoading(true)

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        } catch (e: Exception) {
            isProcessing = false
            showLoading(false)
            showInvalidCodeError()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        lifecycleScope.launch {
            try {
                // Link phone number to account
                auth.currentUser?.linkWithCredential(credential)?.await()

                // Update phone in database
                val updates = mapOf(
                    "phone" to phoneNumber,
                    "username" to currentUsername
                )

                val userId = auth.currentUser?.uid ?: throw Exception("User not found")
                database.getReference("users").child(userId)
                    .updateChildren(updates)
                    .await()

                // Show success screen
                showPhoneVerifiedScreen()
            } catch (e: Exception) {
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        when (e.errorCode) {
                            "ERROR_INVALID_VERIFICATION_CODE" -> {
                                showInvalidCodeError() // This will show the error message
                            }
                            "ERROR_SESSION_EXPIRED" -> {
                                showError("Code expired. Please request a new one.")
                                startResendTimer()
                            }
                            else -> showError("Invalid code. Please try again.")
                        }
                    }
                    else -> showError("Verification failed: ${e.message}")
                }
                isProcessing = false
                showLoading(false)
            }
        }
    }


    private fun showInvalidCodeError() {
        binding.apply {
            // Show error message with red color
            errorText.apply {
                text = "Wrong code! Please try again."
                visibility = View.VISIBLE
                setTextColor(ContextCompat.getColor(this@VerifyPhoneActivity, R.color.error_red))
            }

            // Apply shake animation
            val shake = AnimationUtils.loadAnimation(this@VerifyPhoneActivity, R.anim.shake)
            codeInputLayout.startAnimation(shake)

            // Clear inputs and focus first digit
            digitInputs.forEach { it.setText("") }
            digit1.requestFocus()

            // Disable verify button
            verifyButton.isEnabled = false
        }
    }

    private fun resendVerificationCode() {
        if (isProcessing) return
        isProcessing = true
        showLoading(true)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .apply {
                resendToken?.let { token ->
                    setForceResendingToken(token)
                }
            }
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isProcessing = false
                    showLoading(false)
                    showError("Failed to send code: ${e.message}")
                }

                override fun onCodeSent(
                    newVerificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = newVerificationId
                    resendToken = token
                    isProcessing = false
                    showLoading(false)
                    showSuccess("New verification code sent")
                    startResendTimer()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun startResendTimer() {
        binding.apply {
            resendButton.isEnabled = false
            resendButton.alpha = 0.5f

            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timerSeconds = (millisUntilFinished / 1000).toInt()
                    timerText.text = "Resend code in: ${timerSeconds}s"
                }

                override fun onFinish() {
                    timerSeconds = 0
                    resendButton.isEnabled = true
                    resendButton.alpha = 1.0f
                    timerText.text = "Didn't receive the code?"
                }
            }.start()
        }
    }

    private fun showPhoneVerifiedScreen() {
        val intent = Intent(this, PhoneVerifiedActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Verification")
            .setMessage("Are you sure you want to cancel phone verification?")
            .setPositiveButton("Yes") { _, _ -> navigateBack() }
            .setNegativeButton("No", null)
            .setCancelable(false)
            .show()
    }

    private fun navigateBack() {
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            loadingBackground.visibility = if (show) View.VISIBLE else View.GONE
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            verifyButton.isEnabled = !show && getEnteredCode().length == 6
            digitInputs.forEach { it.isEnabled = !show }
            backButton.isEnabled = !show
            resendButton.isEnabled = !show && timerSeconds <= 0

            if (show) {
                // Clear any existing error when showing loading
                errorText.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        if (!isFinishing) {
            binding.apply {
                errorText.apply {
                    text = message
                    visibility = View.VISIBLE
                    setTextColor(ContextCompat.getColor(this@VerifyPhoneActivity, R.color.error_red))
                }
                noteText.visibility = View.GONE  // Hide note text when showing error
            }
        }
    }

    private fun showSuccess(message: String) {
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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