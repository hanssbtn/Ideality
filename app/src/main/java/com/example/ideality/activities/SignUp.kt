package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ideality.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var phoneInput: EditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var signUpButton: MaterialButton
    private lateinit var signInText: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        initializeFirebase()
        initializeViews()
        setupClickListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
    }

    private fun initializeViews() {
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        phoneInput = findViewById(R.id.phoneInput)
        ccp = findViewById(R.id.ccp)
        signUpButton = findViewById(R.id.signUpButton)
        signInText = findViewById(R.id.signInText)

        ccp.registerCarrierNumberEditText(phoneInput)
    }

    private fun setupClickListeners() {
        signUpButton.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }

        signInText.setOnClickListener {
            // Just finish this activity to go back to Login
            onBackPressed()
        }
    }

    private fun validateInputs(): Boolean {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()

        if (username.isEmpty()) {
            usernameInput.error = "Username is required"
            return false
        }

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return false
        }

        if (phone.isEmpty()) {
            phoneInput.error = "Phone number is required"
            return false
        }

        if (!ccp.isValidFullNumber) {
            phoneInput.error = "Please enter a valid phone number"
            return false
        }

        return true
    }

    private fun registerUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        signUpButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create authentication account
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                // Save additional user data to Firestore
                val userData = hashMapOf(
                    "username" to usernameInput.text.toString().trim(),
                    "email" to email,
                    "phone" to ccp.fullNumberWithPlus,
                    "createdAt" to System.currentTimeMillis()
                )

                authResult.user?.uid?.let { uid ->
                    db.collection("users").document(uid).set(userData).await()
                }

                withContext(Dispatchers.Main) {
                    navigateToMainActivity()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleSignUpError(e)
                    signUpButton.isEnabled = true
                }
            }
        }
    }

    private fun handleSignUpError(e: Exception) {
        val errorMessage = when (e) {
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 6 characters"
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Invalid email format"
            is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                "An account already exists with this email"
            else -> "Registration failed: ${e.message}"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error navigating to main: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
}