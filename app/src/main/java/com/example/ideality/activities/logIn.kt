package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ideality.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LogIn : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: View
    private lateinit var googleLoginBtn: View
    private lateinit var facebookLoginBtn: View
    private lateinit var githubLoginBtn: View
    private lateinit var signUpText: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in)

        initializeViews()
        setupClickListeners()

        auth = FirebaseAuth.getInstance()
        auth.signOut()
        // Check if user is already logged in
        if (auth.currentUser != null) {
            navigateToMainActivity()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        googleLoginBtn = findViewById(R.id.googleLoginBtn)
        facebookLoginBtn = findViewById(R.id.facebookLoginBtn)
        githubLoginBtn = findViewById(R.id.githubLoginBtn)
        signUpText = findViewById(R.id.signUpText)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (validateInputs()) {
                loginUser()
            }
        }

        signUpText.setOnClickListener {
            // Simplest possible navigation
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        googleLoginBtn.setOnClickListener {
            Toast.makeText(this, "Google Sign In coming soon!", Toast.LENGTH_SHORT).show()
        }

        facebookLoginBtn.setOnClickListener {
            Toast.makeText(this, "Facebook Sign In coming soon!", Toast.LENGTH_SHORT).show()
        }

        githubLoginBtn.setOnClickListener {
            Toast.makeText(this, "GitHub Sign In coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(): Boolean {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

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

        return true
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        loginButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                withContext(Dispatchers.Main) {
                    navigateToMainActivity()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleLoginError(e)
                    loginButton.isEnabled = true
                }
            }
        }
    }

    private fun handleLoginError(e: Exception) {
        val errorMessage = when (e) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                "No account exists with this email"
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password"
            else -> "Login failed: ${e.message}"
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
