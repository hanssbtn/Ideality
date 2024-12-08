package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ideality.R
import com.example.ideality.databinding.ActivityEditEmailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditEmailBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isProcessing = false
    private var currentUsername = ""
    private var hasChanges = false
    private var isGoogleUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        loadUserData()
        setupUI()
        setupClickListeners()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        isGoogleUser = auth.currentUser?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } ?: false

        database.getReference("users").child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                currentUsername = snapshot.child("username").value as? String ?: ""
                val authType = snapshot.child("authType").value as? String
                isGoogleUser = authType == "google" || isGoogleUser
            }
    }

    private fun setupUI() {
        binding.apply {
            currentEmailText.text = auth.currentUser?.email

            newEmailInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    hasChanges = s?.toString()?.isNotEmpty() == true
                }
                override fun afterTextChanged(s: Editable?) {
                    validateEmail(showError = false)
                    updateContinueButton()
                }
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
                    initiateEmailChange()
                }
            }
        }
    }

    private fun validateEmail(showError: Boolean = true): Boolean {
        val email = binding.newEmailInput.text.toString().trim()
        val currentEmail = auth.currentUser?.email

        return when {
            email.isEmpty() -> {
                if (showError) showError("Email is required")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                if (showError) showError("Enter a valid email address")
                false
            }
            email == currentEmail -> {
                if (showError) showError("New email must be different from current email")
                false
            }
            else -> true
        }
    }

    private fun validateInputs(): Boolean {
        return validateEmail()
    }

    private fun updateContinueButton() {
        binding.saveButton.isEnabled = binding.newEmailInput.text.toString().isNotEmpty() && !isProcessing
    }

    private fun initiateEmailChange() {
        if (isProcessing) return
        isProcessing = true
        showLoading(true)

        val newEmailInput = binding.newEmailInput.text.toString().trim()
        val currentEmail = auth.currentUser?.email ?: ""

        lifecycleScope.launch {
            try {
                // Configure settings for re-authentication link
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://${packageName}.firebaseapp.com")
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(
                        packageName,
                        true,
                        null
                    )
                    .build()

                // Send verification to current email first
                auth.sendSignInLinkToEmail(currentEmail, actionCodeSettings).await()

                // Save the new email for later
                getSharedPreferences("auth", MODE_PRIVATE)
                    .edit()
                    .putString("pendingNewEmail", newEmailInput)
                    .putString("currentUsername", currentUsername)
                    .putBoolean("isGoogleUser", isGoogleUser)
                    .apply()

                // Show success message
                showSuccess("Please verify your current email first")

                // Navigate to verification screen
                val intent = Intent(this@EditEmailActivity, VerifyCurrentEmailActivity::class.java)
                intent.putExtra("currentEmail", currentEmail)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                showError("Failed to start email verification: ${e.message}")
                showLoading(false)
            }
            isProcessing = false
        }
    }

    private fun showPasswordVerification(newEmail: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_confirm, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Verify Password")
            .setMessage("Please enter your current password to continue")
            .setView(dialogView)
            .setPositiveButton("Confirm") { dialog, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    verifyPasswordAndProceed(password, newEmail)
                } else {
                    passwordInput.error = "Password required"
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                showLoading(false)
                isProcessing = false
            }
            .setCancelable(false)
            .show()
    }

    private fun verifyPasswordAndProceed(password: String, newEmail: String) {
        isProcessing = true
        showLoading(true)

        lifecycleScope.launch {
            try {
                val user = auth.currentUser ?: throw Exception("User not found")
                val credential = EmailAuthProvider.getCredential(user.email!!, password)

                // Re-authenticate
                user.reauthenticate(credential).await()

                // If successful, proceed to verification
                proceedToVerification(newEmail)
            } catch (e: Exception) {
                showError("Authentication failed. Please check your password.")
                showLoading(false)
                isProcessing = false
            }
        }
    }

    private fun proceedToVerification(newEmail: String) {
        val intent = Intent(this, VerifyEmailUpdateActivity::class.java).apply {
            putExtra("email", auth.currentUser?.email)
            putExtra("newEmail", newEmail)
            putExtra("username", currentUsername)
            putExtra("isGoogleUser", isGoogleUser)
        }
        startActivity(intent)
        finish()
    }

    private fun onBackPressedHandler() {
        if (isProcessing) {
            showError("Please wait while processing...")
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
        super.onBackPressed()
        onBackPressedHandler()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            saveButton.isEnabled = !show
            newEmailInput.isEnabled = !show
            backButton.isEnabled = !show
            saveButton.alpha = if (show) 0.5f else 1f
        }
    }

    private fun showSuccess(message: String) {
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}