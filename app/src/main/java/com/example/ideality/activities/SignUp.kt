package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ideality.R
import com.example.ideality.models.UserData
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference

    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var usernameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var signUpButton: MaterialButton
    private lateinit var signInText: View
    private lateinit var progressBar: ProgressBar
    private lateinit var passwordStrengthView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        initializeFirebase()
        initializeViews()
        setupClickListeners()
        setupPasswordStrengthIndicator()
        setupRealTimeValidation()
    }

    private fun setupClickListeners() {
        signUpButton.setOnClickListener {
            clearErrors()
            if (validateInputs()) {
                registerUser()
            }
        }

        signInText.setOnClickListener {
            finish() // Return to login screen
        }
    }

    private fun clearErrors() {
        usernameInputLayout.error = null
        emailInputLayout.error = null
        passwordInputLayout.error = null
        confirmPasswordInputLayout.error = null
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
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        usernameInputLayout = findViewById(R.id.usernameInputLayout)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout)
        signUpButton = findViewById(R.id.signUpButton)
        signInText = findViewById(R.id.signInText)
        progressBar = findViewById(R.id.progressBar)
        passwordStrengthView = findViewById(R.id.passwordStrengthView)
    }

    private fun setupRealTimeValidation() {
        // Username validation
        usernameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateUsername(showError = false)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Email validation
        emailInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateEmail(showError = false)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Confirm password validation
        confirmPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validatePasswordMatch()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupPasswordStrengthIndicator() {
        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                when {
                    password.isEmpty() -> {
                        passwordInputLayout.helperText = "Enter password"
                        passwordInputLayout.setHelperTextColor(ContextCompat.getColorStateList(this@SignUp, R.color.gray))
                        passwordStrengthView.setBackgroundColor(ContextCompat.getColor(this@SignUp, R.color.gray))
                    }
                    password.length < 6 -> {
                        passwordInputLayout.helperText = "Weak password"
                        passwordInputLayout.setHelperTextColor(ContextCompat.getColorStateList(this@SignUp, R.color.red))
                        passwordStrengthView.setBackgroundColor(ContextCompat.getColor(this@SignUp, R.color.red))
                    }
                    !password.matches(".*[A-Z].*".toRegex()) -> {
                        passwordInputLayout.helperText = "Add uppercase letter"
                        passwordInputLayout.setHelperTextColor(ContextCompat.getColorStateList(this@SignUp, R.color.orange))
                        passwordStrengthView.setBackgroundColor(ContextCompat.getColor(this@SignUp, R.color.orange))
                    }
                    !password.matches(".*[a-z].*".toRegex()) -> {
                        passwordInputLayout.helperText = "Add lowercase letter"
                        passwordInputLayout.setHelperTextColor(ContextCompat.getColorStateList(this@SignUp, R.color.orange))
                        passwordStrengthView.setBackgroundColor(ContextCompat.getColor(this@SignUp, R.color.orange))
                    }
                    !password.matches(".*[0-9].*".toRegex()) -> {
                        passwordInputLayout.helperText = "Add a number"
                        passwordInputLayout.setHelperTextColor(ContextCompat.getColorStateList(this@SignUp, R.color.orange))
                        passwordStrengthView.setBackgroundColor(ContextCompat.getColor(this@SignUp, R.color.orange))
                    }
                    password.length < 8 -> {
                        passwordInputLayout.helperText = "Medium strength"
                        passwordInputLayout.setHelperTextColor(ContextCompat.getColorStateList(this@SignUp, R.color.orange))
                        passwordStrengthView.setBackgroundColor(ContextCompat.getColor(this@SignUp, R.color.orange))
                    }
                    else -> {
                        passwordInputLayout.helperText = "Strong password"
                        passwordInputLayout.setHelperTextColor(ContextCompat.getColorStateList(this@SignUp, R.color.green))
                        passwordStrengthView.setBackgroundColor(ContextCompat.getColor(this@SignUp, R.color.green))
                    }
                }
                validatePasswordMatch()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validatePasswordMatch() {
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        if (confirmPassword.isNotEmpty()) {
            if (password != confirmPassword) {
                confirmPasswordInputLayout.error = "Passwords do not match"
            } else {
                confirmPasswordInputLayout.error = null
            }
        }
    }

    private fun validateUsername(showError: Boolean = true): Boolean {
        val username = usernameInput.text.toString().trim()
        return when {
            username.isEmpty() -> {
                if (showError) usernameInputLayout.error = "Username is required"
                false
            }
            username.length < 3 -> {
                if (showError) usernameInputLayout.error = "Username must be at least 3 characters"
                false
            }
            !username.matches("[a-zA-Z0-9._-]+".toRegex()) -> {
                if (showError) usernameInputLayout.error = "Username can only contain letters, numbers, dots, underscores and hyphens"
                false
            }
            else -> {
                usernameInputLayout.error = null
                true
            }
        }
    }

    private fun validateEmail(showError: Boolean = true): Boolean {
        val email = emailInput.text.toString().trim()
        return when {
            email.isEmpty() -> {
                if (showError) emailInputLayout.error = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                if (showError) emailInputLayout.error = "Please enter a valid email"
                false
            }
            else -> {
                emailInputLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        return when {
            password.isEmpty() -> {
                passwordInputLayout.error = "Password is required"
                false
            }
            password.length < 6 -> {
                passwordInputLayout.error = "Password must be at least 6 characters"
                false
            }
            confirmPassword.isEmpty() -> {
                confirmPasswordInputLayout.error = "Please confirm your password"
                false
            }
            password != confirmPassword -> {
                confirmPasswordInputLayout.error = "Passwords do not match"
                false
            }
            else -> {
                passwordInputLayout.error = null
                confirmPasswordInputLayout.error = null
                true
            }
        }
    }

    private fun validateInputs(): Boolean {
        return validateUsername() && validateEmail() && validatePassword()
    }

    // Rest of your existing code remains the same...

    private fun registerUser() {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        showLoading(true)

        // First check if username exists
        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        showLoading(false)
                        usernameInputLayout.error = "Username already exists"
                    } else {
                        // Check if email exists
                        usersRef.orderByChild("email").equalTo(email)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        showLoading(false)
                                        emailInputLayout.error = "Email already registered"
                                    } else {
                                        proceedWithRegistration(username, email, password)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    showLoading(false)
                                    showError("Connection error. Please try again.")
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Connection error. Please try again.")
                }
            })
    }

    private fun proceedWithRegistration(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // Send verification email first
                        user.sendEmailVerification()
                            .addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    val userData = UserData(
                                        uid = user.uid,
                                        username = username,
                                        email = email,
                                        phone = "",
                                        password = password,
                                        createdAt = System.currentTimeMillis(),
                                        googleLinked = false,
                                        authType = "email"
                                    )

                                    usersRef.child(it.uid).setValue(userData)
                                        .addOnCompleteListener { dbTask ->
                                            showLoading(false)
                                            if (dbTask.isSuccessful) {
                                                // Start verification screen instead of showing success message
                                                val intent = Intent(this, VerifyEmailActivity::class.java)
                                                intent.putExtra("email", email)
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                showError("Failed to save user data. Please try again.")
                                            }
                                        }
                                } else {
                                    showLoading(false)
                                    showError("Failed to send verification email. Please try again.")
                                }
                            }
                    }
                } else {
                    showLoading(false)
                    when (task.exception?.message) {
                        "The email address is already in use by another account." -> {
                            emailInputLayout.error = "Email already registered"
                        }
                        "The email address is badly formatted." -> {
                            emailInputLayout.error = "Invalid email format"
                        }
                        "The given password is invalid. [ Password should be at least 6 characters ]" -> {
                            passwordInputLayout.error = "Password must be at least 6 characters"
                        }
                        else -> {
                            showError("Registration failed. Please try again.")
                        }
                    }
                }
            }
    }

    private fun showLoading(show: Boolean) {
        findViewById<View>(R.id.loadingBackground).visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE

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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
