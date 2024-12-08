package com.example.ideality.activities

import android.content.Intent
import com.google.android.material.textfield.TextInputLayout
import android.os.Bundle
import android.os.Handler
import com.google.firebase.auth.AuthCredential
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.R
import com.example.ideality.models.UserData
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.CheckBox

class LogIn : AppCompatActivity() {
    private val RC_SIGN_IN = 9001

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient
    private var loginAttempts = 0
    private val MAX_LOGIN_ATTEMPTS = 5
    private lateinit var rememberMeCheckbox: CheckBox

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var signUpText: View
    private lateinit var googleLoginBtn: MaterialButton
    private lateinit var forgotPasswordText: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        initializeFirebase()
        configureGoogleSignIn()
        initializeViews()
        setupClickListeners()
        setupRememberMe()
        checkCurrentUser()



        if (!checkNetworkConnection()) {
            showError("Please check your internet connection")
        }
    }

    private fun updatePasswordInDatabase(uid: String, newPassword: String) {
        usersRef.child(uid).child("password").setValue(newPassword)
            .addOnSuccessListener {
                Log.d("PasswordUpdate", "Password updated in database")
            }
            .addOnFailureListener { e ->
                Log.e("PasswordUpdate", "Failed to update password in database", e)
            }
    }

    private fun configureGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
            Log.d("GoogleSignIn", "Google Sign In configured successfully")
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Error configuring Google Sign In", e)
            showError("Error configuring Google Sign In: ${e.message}")
        }
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBar)
        signUpText = findViewById(R.id.signUpText)
        googleLoginBtn = findViewById(R.id.googleLoginBtn)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let {
            startActivity(Intent(this, Home::class.java))
            finish()
        }
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            clearErrors()
            if (validateInputs()) {
                loginUser()
            }
        }

        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        googleLoginBtn.setOnClickListener {
            signInWithGoogle()
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun clearErrors() {
        emailInputLayout.error = null
        passwordInputLayout.error = null
    }

    private fun signInWithGoogle() {
        showLoading(true)
        try {
            // Sign out before starting new sign in to ensure clean state
            auth.signOut()
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
                Log.d("GoogleSignIn", "Started Google Sign In flow")
            }
        } catch (e: Exception) {
            showLoading(false)
            Log.e("GoogleSignIn", "Error starting Google Sign In", e)
            showError("Error starting Google Sign In: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            Log.d("GoogleSignIn", "Got activity result for Google Sign In")
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleGoogleSignInResult(task)
            } catch (e: Exception) {
                showLoading(false)
                Log.e("GoogleSignIn", "Error in Google Sign In result", e)
                showError("Google sign in failed: ${e.message}")
            }
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d("GoogleSignIn", "Got Google account, email: ${account.email}")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            showLoading(false)
            Log.e("GoogleSignIn", "Google sign in failed", e)
            showError("Google sign in failed. Error code: ${e.statusCode}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("GoogleSignIn", "Starting Firebase auth with Google")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
        val email = googleSignInAccount?.email

        if (email != null) {
            // First check if email exists in our database
            usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val existingData = snapshot.children.first().getValue(UserData::class.java)
                            Log.d("GoogleSignIn", "Found existing account: ${existingData?.googleLinked}")

                            if (existingData?.googleLinked == false) {
                                // This is an email account, try to link it
                                linkAccounts(credential, existingData)
                            } else {
                                // Already a Google account, just sign in
                                proceedWithGoogleSignIn(credential)
                            }
                        } else {
                            // New user, create account
                            proceedWithGoogleSignIn(credential)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showLoading(false)
                        Log.e("GoogleSignIn", "Database error", error.toException())
                        showError("Database error: ${error.message}")
                    }
                })
        }
    }

    private fun proceedWithGoogleSignIn(credential: AuthCredential) {
        Log.d("GoogleSignIn", "Starting Firebase sign in with credential")
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                Log.d("GoogleSignIn", "Firebase auth successful")
                updateUserData(result.user)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e("GoogleSignIn", "Firebase auth failed", e)
                showError("Google sign in failed: ${e.message}")
            }
    }

    private fun linkAccounts(googleCredential: AuthCredential, existingData: UserData?) {
        if (existingData == null) {
            showLoading(false)
            showError("Failed to link accounts: Missing user data")
            return
        }

        auth.signInWithEmailAndPassword(existingData.email, existingData.password)
            .addOnSuccessListener { result ->
                val emailUser = result.user

                emailUser?.linkWithCredential(googleCredential)
                    ?.addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d("Account", "linkWithCredential:success")

                            // Update user data to reflect both auth methods
                            val updatedData = existingData.copy(
                                googleLinked = true,
                                authType = "both"  // This is important
                            )

                            usersRef.child(emailUser.uid).setValue(updatedData)
                                .addOnSuccessListener {
                                    showLoading(false)
                                    startActivity(Intent(this, Home::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    showLoading(false)
                                    showError("Failed to update user data: ${e.message}")
                                }
                        } else {
                            Log.w("Account", "linkWithCredential:failure", task.exception)
                            showLoading(false)
                            showError("Failed to link accounts: ${task.exception?.message}")
                        }
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Authentication failed: ${e.message}")
            }
    }

    private fun updateUserData(firebaseUser: FirebaseUser?) {
        firebaseUser?.let { user ->
            usersRef.orderByChild("email").equalTo(user.email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val existingData = snapshot.children.first().getValue(UserData::class.java)

                            val updatedData = UserData(
                                uid = user.uid,
                                username = existingData?.username ?: user.displayName ?: "",
                                email = user.email ?: "",
                                password = existingData?.password ?: "",
                                phone = existingData?.phone ?: "",
                                googleLinked = true,
                                authType = existingData?.authType ?: "google",
                                createdAt = existingData?.createdAt ?: System.currentTimeMillis()
                            )

                            usersRef.child(user.uid).setValue(updatedData)
                                .addOnSuccessListener {
                                    showLoading(false)
                                    startActivity(Intent(this@LogIn, Home::class.java))
                                    finish()
                                }
                        } else {
                            val userData = UserData(
                                uid = user.uid,
                                username = user.displayName ?: "",
                                email = user.email ?: "",
                                password = "",
                                phone = "",
                                googleLinked = true,
                                authType = "google",
                                createdAt = System.currentTimeMillis()
                            )

                            usersRef.child(user.uid).setValue(userData)
                                .addOnSuccessListener {
                                    showLoading(false)
                                    startActivity(Intent(this@LogIn, Home::class.java))
                                    finish()
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showLoading(false)
                        showError("Database error: ${error.message}")
                    }
                })
        }
    }

    private fun validateInputs(): Boolean {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        var isValid = true

        if (email.isEmpty()) {
            emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        showLoading(true)

        usersRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userData = snapshot.children.first().getValue(UserData::class.java)

                        when (userData?.authType) {
                            "google" -> {
                                showLoading(false)
                                emailInputLayout.error = "This email was registered with Google. Please use Google Sign In"
                                passwordInputLayout.error = null
                            }
                            "both", "email" -> {
                                proceedWithEmailLogin(email, password)
                            }
                            else -> {
                                proceedWithEmailLogin(email, password)
                            }
                        }
                    } else {
                        showLoading(false)
                        emailInputLayout.error = "No account found with this email"
                        passwordInputLayout.error = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Connection error. Please try again.")
                }
            })
    }

    private fun proceedWithEmailLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    // Update password in database if it was reset
                    val user = auth.currentUser
                    if (user != null) {
                        usersRef.child(user.uid).child("password").get()
                            .addOnSuccessListener { snapshot ->
                                val storedPassword = snapshot.value as? String
                                if (storedPassword.isNullOrEmpty()) {
                                    // Password was reset, update it in database
                                    updatePasswordInDatabase(user.uid, password)
                                }
                                startActivity(Intent(this, Home::class.java))
                                finish()
                            }
                    }
                } else {
                    handleFailedLogin()
                    when (task.exception?.message) {
                        "The password is invalid or the user does not have a password." -> {
                            passwordInputLayout.error = "Incorrect password"
                            emailInputLayout.error = null
                        }
                        "The email address is badly formatted." -> {
                            emailInputLayout.error = "Invalid email format"
                            passwordInputLayout.error = null
                        }
                        else -> {
                            passwordInputLayout.error = "Login failed. Please try again."
                            emailInputLayout.error = null
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

    private fun validateEmail(): Boolean {
        val email = emailInput.text.toString().trim()
        return when {
            email.isEmpty() -> {
                emailInputLayout.error = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailInputLayout.error = "Please enter a valid email"
                false
            }
            else -> {
                emailInputLayout.error = null
                true
            }
        }
    }

    private fun handleFailedLogin() {
        loginAttempts++
        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            loginButton.isEnabled = false
            showError("Too many failed attempts. Please try again later or reset your password.")
            Handler(Looper.getMainLooper()).postDelayed({
                loginAttempts = 0
                loginButton.isEnabled = true
            }, 30000) // 30 seconds timeout
        }
    }

    private fun setupRememberMe() {
        val sharedPref = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        rememberMeCheckbox.isChecked = sharedPref.getBoolean("remember_me", false)

        if (rememberMeCheckbox.isChecked) {
            emailInput.setText(sharedPref.getString("email", ""))
        }

        rememberMeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().apply {
                putBoolean("remember_me", isChecked)
                if (!isChecked) {
                    remove("email")
                }
                apply()
            }
        }
    }

    private fun checkNetworkConnection(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}