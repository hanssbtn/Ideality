package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.R
import com.example.ideality.models.UserData
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser

class LogIn : AppCompatActivity() {
    // Add request code for Google Sign In
    private val RC_SIGN_IN = 9001

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var signUpText: View
    private lateinit var googleLoginBtn: View
    private lateinit var facebookLoginBtn: View
    private lateinit var githubLoginBtn: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        initializeFirebase()
        configureGoogleSignIn()  // Add this line
        initializeViews()
        setupClickListeners()
        checkCurrentUser()
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("270702607846-908dv5dqkkjmk5dmg741fv0o9rioeu08.apps.googleusercontent.com")  // Web client ID from your JSON
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")

        auth.signOut()
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBar)

        // Add these lines
        signUpText = findViewById(R.id.signUpText)
        googleLoginBtn = findViewById(R.id.googleLoginBtn)
        facebookLoginBtn = findViewById(R.id.facebookLoginBtn)
        githubLoginBtn = findViewById(R.id.githubLoginBtn)
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (validateInputs()) {
                loginUser()
            }
        }

        findViewById<View>(R.id.signUpText).setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        // Update Google login button click listener
        findViewById<View>(R.id.googleLoginBtn).setOnClickListener {
            signInWithGoogle()
        }

        findViewById<View>(R.id.facebookLoginBtn).setOnClickListener {
            Toast.makeText(this, "Facebook Sign In coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.githubLoginBtn).setOnClickListener {
            Toast.makeText(this, "GitHub Sign In coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleGoogleSignInResult(task)
            } catch (e: Exception) {
                showError("Google sign in failed: ${e.message}")
            }
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            showError("Google sign in failed: ${e.statusCode}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToDatabase(user)
                } else {
                    showLoading(false)
                    showError("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToDatabase(firebaseUser: FirebaseUser?) {
        firebaseUser?.let { user ->
            val userData = UserData(
                uid = user.uid,
                username = user.displayName ?: "",
                email = user.email ?: "",
                phone = user.phoneNumber ?: "",
                createdAt = System.currentTimeMillis()
            )

            usersRef.child(user.uid).setValue(userData)
                .addOnCompleteListener { task ->
                    showLoading(false)
                    if (task.isSuccessful) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        showError("Failed to save user data")
                    }
                }
        }
    }

    private fun validateInputs(): Boolean {
        val emailOrUsername = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (emailOrUsername.isEmpty()) {
            emailInput.error = "Email or username is required"
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }

        return true
    }

    private fun loginUser() {
        val emailOrUsername = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        showLoading(true)

        // Check if input is email or username
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
            // Login with email
            loginWithEmail(emailOrUsername, password)
        } else {
            // Login with username
            loginWithUsername(emailOrUsername, password)
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showError(task.exception?.message ?: "Login failed")
                }
            }
    }

    private fun loginWithUsername(username: String, password: String) {
        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userData = snapshot.children.first().getValue(UserData::class.java)
                        userData?.let { user ->
                            loginWithEmail(user.email, password)
                        }
                    } else {
                        showLoading(false)
                        showError("Username not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Database error: ${error.message}")
                }
            })
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
}