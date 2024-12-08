package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ideality.databinding.ActivityEditPhoneBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class EditPhoneActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditPhoneBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isProcessing = false
    private var hasChanges = false
    private var currentUsername = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        loadUserData()
        setupUI()
        setupClickListeners()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Add this for testing
        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users").child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                currentUsername = snapshot.child("username").value as? String ?: ""
                val currentPhone = snapshot.child("phone").value as? String ?: ""
                binding.currentPhoneText.text = currentPhone.ifEmpty { "No phone number added" }
            }
    }

    private fun setupUI() {
        binding.apply {
            // Setup Country Code Picker
            ccp.registerCarrierNumberEditText(newPhoneInput)

            // Setup error text views
            phoneInputLayout.error = null  // Clear any initial errors

            // Watch for changes
            newPhoneInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    hasChanges = true
                    updateContinueButton()
                    // Clear error when user starts typing
                    phoneInputLayout.error = null
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedHandler()
            }

            saveButton.setOnClickListener {
                if (!isProcessing && validateInputs()) {
                    sendVerificationCode()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val phoneNumber = binding.newPhoneInput.text?.toString() ?: ""
        binding.apply {
            return when {
                phoneNumber.isEmpty() -> {
                    phoneInputLayout.error = "Please enter a phone number"
                    false
                }
                phoneNumber.length < 6 -> {
                    phoneInputLayout.error = "Phone number is too short"
                    false
                }
                !ccp.isValidFullNumber -> {
                    phoneInputLayout.error = "Please enter a valid phone number"
                    false
                }
                ccp.fullNumberWithPlus == currentPhoneText.text?.toString() -> {
                    phoneInputLayout.error = "This is already your current phone number"
                    false
                }
                else -> {
                    phoneInputLayout.error = null
                    true
                }
            }
        }
    }

    private fun updateContinueButton() {
        binding.saveButton.isEnabled = binding.newPhoneInput.text?.isNotEmpty() == true && !isProcessing
    }

    private fun sendVerificationCode() {
        if (isProcessing) return
        isProcessing = true
        showLoading(true)

        val phoneNumber = binding.ccp.fullNumberWithPlus

        checkPhoneNumberExists(phoneNumber) { exists ->
            if (exists) {
                isProcessing = false
                showLoading(false)
                binding.phoneInputLayout.error = "This phone number is already linked to another account"
                return@checkPhoneNumberExists
            }

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
                        verifyPhoneNumberCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        isProcessing = false
                        showLoading(false)

                        binding.phoneInputLayout.error = when (e) {
                            is FirebaseAuthInvalidCredentialsException -> {
                                when (e.errorCode) {
                                    "ERROR_INVALID_PHONE_NUMBER" -> "Invalid phone number format"
                                    "ERROR_MISSING_PHONE_NUMBER" -> "Please enter a phone number"
                                    else -> "Invalid phone number: ${e.message}"
                                }
                            }
                            is FirebaseTooManyRequestsException -> "Too many requests. Please try again later"
                            is FirebaseAuthMissingActivityForRecaptchaException -> "Error with verification. Please try again"
                            else -> "Verification failed: ${e.message}"
                        }
                    }

                    override fun onCodeSent(
                        verId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        verificationId = verId
                        resendToken = token
                        showSuccess("Verification code sent")

                        // Navigate to verification screen
                        val intent = Intent(this@EditPhoneActivity, VerifyPhoneActivity::class.java)
                        intent.putExtra("verificationId", verId)
                        intent.putExtra("phoneNumber", phoneNumber)
                        intent.putExtra("username", currentUsername)
                        startActivity(intent)
                        isProcessing = false
                        showLoading(false)
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    private fun checkPhoneNumberExists(phoneNumber: String, callback: (Boolean) -> Unit) {
        database.getReference("users")
            .orderByChild("phone")
            .equalTo(phoneNumber)
            .get()
            .addOnSuccessListener { snapshot ->
                val exists = snapshot.exists() && snapshot.children.none {
                    it.key == auth.currentUser?.uid
                }
                callback(exists)
            }
            .addOnFailureListener {
                callback(false)
                binding.phoneInputLayout.error = "Error checking phone number: ${it.message}"
            }
    }

    private fun verifyPhoneNumberCredential(credential: PhoneAuthCredential) {
        lifecycleScope.launch {
            try {
                auth.currentUser?.linkWithCredential(credential)?.await()

                val userId = auth.currentUser?.uid ?: return@launch
                database.getReference("users").child(userId)
                    .child("phone")
                    .setValue(binding.ccp.fullNumberWithPlus)
                    .await()

                showSuccess("Phone number verified successfully")
                finish()
            } catch (e: Exception) {
                binding.phoneInputLayout.error = "Failed to verify phone: ${e.message}"
            } finally {
                isProcessing = false
                showLoading(false)
            }
        }
    }

    private fun onBackPressedHandler() {
        if (isProcessing) {
            binding.phoneInputLayout.error = "Please wait while processing..."
            return
        }

        if (hasChanges) {
            showExitConfirmationDialog()
        } else {
            finish()
        }
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to discard your changes?")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        onBackPressedHandler()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            loadingBackground.visibility = if (show) View.VISIBLE else View.GONE
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            saveButton.isEnabled = !show
            newPhoneInput.isEnabled = !show
            backButton.isEnabled = !show
            saveButton.alpha = if (show) 0.5f else 1f
        }
    }

    // Only use Toast for success messages
    private fun showSuccess(message: String) {
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}