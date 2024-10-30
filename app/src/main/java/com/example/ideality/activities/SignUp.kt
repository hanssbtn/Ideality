package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.R
import com.example.ideality.models.UserData  // Make sure UserData is in the models package
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference


    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var phoneInput: EditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var signUpButton: MaterialButton
    private lateinit var signInText: View
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        initializeFirebase()
        initializeViews()
        setupClickListeners()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")
    }

    private fun initializeViews() {
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        phoneInput = findViewById(R.id.phoneInput)
        ccp = findViewById(R.id.ccp)
        signUpButton = findViewById(R.id.signUpButton)
        signInText = findViewById(R.id.signInText)
        progressBar = findViewById(R.id.progressBar)

        ccp.registerCarrierNumberEditText(phoneInput)
    }

    private fun setupClickListeners() {
        signUpButton.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }

        signInText.setOnClickListener {
            finish() // Return to login screen
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

        if (username.length < 3) {
            usernameInput.error = "Username must be at least 3 characters"
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
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val phone = ccp.fullNumberWithPlus

        showLoading(true)

        // First check if username exists
        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        showLoading(false)
                        usernameInput.error = "Username already exists"
                    } else {
                        // Username is available, proceed with registration
                        proceedWithRegistration(username, email, password, phone)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Database error: ${error.message}")
                }
            })
    }

    private fun proceedWithRegistration(username: String, email: String, password: String, phone: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userData = UserData(
                            uid = it.uid,
                            username = username,
                            email = email,
                            phone = phone,
                            password = password // In production, use proper encryption
                        )

                        // Save to Realtime Database
                        usersRef.child(it.uid).setValue(userData)
                            .addOnCompleteListener { dbTask ->
                                showLoading(false)
                                if (dbTask.isSuccessful) {
                                    showSuccess("Registration successful!")
                                    // Navigate to login
                                    finish()
                                } else {
                                    showError("Failed to save user data")
                                }
                            }
                    }
                } else {
                    showLoading(false)
                    showError(task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun showLoading(show: Boolean) {
        findViewById<View>(R.id.loadingBackground).visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE

        // Disable user interaction while loading
        if (show) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}