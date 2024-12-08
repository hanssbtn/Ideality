package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log // Add this import
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase // Add this declaration
    private lateinit var usersRef: DatabaseReference // Add this declaration
    private lateinit var emailInput: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var resetButton: MaterialButton
    private lateinit var backButton: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingBackground: View
    private lateinit var bottomSheet: BottomSheetBehavior<View>
    private lateinit var bottomSheetView: View
    private lateinit var openEmailButton: MaterialButton
    private lateinit var scrimBackground: View
    private lateinit var closeButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initializeFirebase()
        initializeViews()
        setupBottomSheet()
        setupClickListeners()
    }


    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")
    }

    private fun initializeViews() {
        try {
            emailInput = findViewById(R.id.emailInput)
            emailInputLayout = findViewById(R.id.emailInputLayout)
            resetButton = findViewById(R.id.resetButton)
            backButton = findViewById(R.id.backButton)
            progressBar = findViewById(R.id.progressBar)
            loadingBackground = findViewById(R.id.loadingBackground)
            bottomSheetView = findViewById(R.id.bottomSheet)
            openEmailButton = findViewById(R.id.openEmailButton)
            scrimBackground = findViewById(R.id.scrimBackground)
            closeButton = findViewById(R.id.closeButton)
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Error initializing views")
            finish()
        }
    }

    private fun setupBottomSheet() {
        bottomSheet = BottomSheetBehavior.from(bottomSheetView).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            isFitToContents = false  // Add this
            expandedOffset = 200  // Add this - adjusts the expanded position
        }

        bottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        scrimBackground.visibility = View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        scrimBackground.visibility = View.GONE
                        finish() // Return to login screen
                    }
                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Adjust scrim alpha based on slide offset
                scrimBackground.alpha = slideOffset.coerceIn(0f, 1f)
            }
        })

        // Handle clicking outside the bottom sheet
        scrimBackground.setOnClickListener {
            dismissBottomSheet()
        }
    }

    private fun setupClickListeners() {
        resetButton.setOnClickListener {
            if (validateEmail()) {
                sendPasswordResetEmail()
            }
        }

        backButton.setOnClickListener {
            finish()
        }

        openEmailButton.setOnClickListener {
            openEmailApp()
        }

        closeButton.setOnClickListener {
            dismissBottomSheet()
        }

        openEmailButton.setOnClickListener {
            openEmailApp()
            dismissBottomSheet()
        }
    }

    private fun validateEmail(): Boolean {
        val email = emailInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInputLayout.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Please enter a valid email"
            return false
        }

        emailInputLayout.error = null
        return true
    }

    private fun sendPasswordResetEmail() {
        val email = emailInput.text.toString().trim()
        showLoading(true)

        // First, check if the email exists in our database
        usersRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Email exists, send reset email
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                showLoading(false)
                                if (task.isSuccessful) {
                                    // Store the email in SharedPreferences to check later
                                    getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("reset_email", email)
                                        .apply()

                                    showBottomSheet()
                                } else {
                                    showError(task.exception?.message ?: "Failed to send reset email")
                                }
                            }
                    } else {
                        showLoading(false)
                        emailInputLayout.error = "No account found with this email"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Database error: ${error.message}")
                }
            })
    }

    private fun showBottomSheet() {
        bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun openEmailApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)

            // Start monitoring for password change
            startPasswordChangeMonitoring()
        } catch (e: Exception) {
            showError("No email app found")
        }
    }

    private fun showLoading(show: Boolean) {
        loadingBackground.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE

        if (show) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            resetButton.isEnabled = false
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            resetButton.isEnabled = true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            super.onBackPressed()
        }
    }

    private fun dismissBottomSheet() {
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun startPasswordChangeMonitoring() {
        // Get the email we sent the reset to
        val resetEmail = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .getString("reset_email", null)

        if (resetEmail != null) {
            // Set up an AuthStateListener to detect when user signs in with new password
            val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null && user.email == resetEmail) {
                    // User has signed in with new password, update database
                    updatePasswordInDatabase(user.uid)
                    // Clear the stored email
                    getSharedPreferences("auth_prefs", MODE_PRIVATE)
                        .edit()
                        .remove("reset_email")
                        .apply()
                }
            }

            // Add the listener
            auth.addAuthStateListener(authStateListener)

            // Remove the listener after 10 minutes (or adjust timing as needed)
            android.os.Handler().postDelayed({
                auth.removeAuthStateListener(authStateListener)
            }, 600000) // 10 minutes
        }
    }
    private fun updatePasswordInDatabase(userId: String) {
        // Get the current user from auth
        val user = auth.currentUser

        user?.getIdToken(true)?.addOnSuccessListener { result ->
            // This token can be used to verify the user on your server
            // Update the password field in database to empty string or a placeholder
            // since we don't have access to the actual new password
            usersRef.child(userId).child("password").setValue("")
                .addOnSuccessListener {
                    Log.d("PasswordReset", "Database updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("PasswordReset", "Failed to update database", e)
                }
        }
    }



}