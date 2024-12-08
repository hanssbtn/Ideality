package com.example.ideality.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.databinding.ActivityChangePasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var auth: FirebaseAuth
    private var isProcessing = false
    private var hasChanges = false

    // Password validation pattern
    private val passwordPattern = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupUI()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupUI() {
        binding.apply {
            // Clear any initial errors
            currentPasswordLayout.error = null
            newPasswordLayout.error = null
            confirmPasswordLayout.error = null
            saveButton.isEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedHandler()
            }

            saveButton.setOnClickListener {
                if (!isProcessing && validateInputs()) {
                    changePassword()
                }
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                hasChanges = true
                updateSaveButton()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.apply {
            currentPasswordInput.addTextChangedListener(textWatcher)
            newPasswordInput.addTextChangedListener(textWatcher)
            confirmPasswordInput.addTextChangedListener(textWatcher)
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        binding.apply {
            // Reset errors
            currentPasswordLayout.error = null
            newPasswordLayout.error = null
            confirmPasswordLayout.error = null

            val currentPassword = currentPasswordInput.text.toString()
            val newPassword = newPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            // Validate current password
            if (currentPassword.isEmpty()) {
                currentPasswordLayout.error = "Please enter your current password"
                isValid = false
            }

            // Validate new password
            if (newPassword.isEmpty()) {
                newPasswordLayout.error = "Please enter a new password"
                isValid = false
            } else if (!passwordPattern.matcher(newPassword).matches()) {
                newPasswordLayout.error = "Password does not meet requirements"
                isValid = false
            }

            // Validate confirm password
            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.error = "Please confirm your new password"
                isValid = false
            } else if (newPassword != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
                isValid = false
            }

            // Check if new password is different from current
            if (currentPassword == newPassword) {
                newPasswordLayout.error = "New password must be different from current password"
                isValid = false
            }
        }
        return isValid
    }

    private fun changePassword() {
        isProcessing = true
        showLoading(true)

        val user = auth.currentUser ?: return
        val currentPassword = binding.currentPasswordInput.text.toString()
        val newPassword = binding.newPasswordInput.text.toString()

        // First, re-authenticate the user
        val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // After re-authentication, update the password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        showSuccess()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        showError("Failed to update password: ${e.message}")
                        isProcessing = false
                        showLoading(false)
                    }
            }
            .addOnFailureListener { e ->
                when (e.message) {
                    "ERROR_WRONG_PASSWORD" -> {
                        binding.currentPasswordLayout.error = "Current password is incorrect"
                    }
                    else -> {
                        showError("Authentication failed: ${e.message}")
                    }
                }
                isProcessing = false
                showLoading(false)
            }
    }

    private fun updateSaveButton() {
        binding.saveButton.isEnabled = hasChanges && !isProcessing &&
                binding.currentPasswordInput.text?.isNotEmpty() == true &&
                binding.newPasswordInput.text?.isNotEmpty() == true &&
                binding.confirmPasswordInput.text?.isNotEmpty() == true
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
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            loadingBackground.visibility = if (show) View.VISIBLE else View.GONE
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            saveButton.isEnabled = !show && hasChanges

            // Disable all inputs while loading
            currentPasswordInput.isEnabled = !show
            newPasswordInput.isEnabled = !show
            confirmPasswordInput.isEnabled = !show
            backButton.isEnabled = !show
        }
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccess() {
        if (!isFinishing) {
            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        onBackPressedHandler()
    }
}
