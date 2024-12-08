package com.example.ideality.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.example.ideality.R
import com.example.ideality.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeVariables()
        setupUI()
        setupClickListeners()
    }

    private fun initializeVariables() {
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
    }

    private fun setupUI() {
        // Set app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val versionName = packageInfo.versionName
            binding.versionText.text = "v$versionName ($versionCode)"
        } catch (e: Exception) {
            binding.versionText.text = "Version Unknown"
        }


        binding.notificationSwitch.apply {
            thumbTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    ContextCompat.getColor(context, R.color.switch_active), // Blue when checked
                    ContextCompat.getColor(context, R.color.switch_inactive)  // Gray when unchecked
                )
            )
            trackTintList = thumbTintList
        }

        // Set notification switch state
        binding.notificationSwitch.isChecked =
            sharedPreferences.getBoolean("notifications_enabled", true)



    }

    private fun setupClickListeners() {
        binding.apply {
            // Back button
            backButton.setOnClickListener {
                finish()
            }

            // Notifications toggle
            notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                handleNotificationToggle(isChecked)
            }

            // Change password
            changePasswordLayout.setOnClickListener {
                // Verify user is signed in with email/password
                val user = auth.currentUser
                if (user?.providerData?.any { it.providerId == "password" } == true) {
                    startActivity(Intent(this@SettingsActivity, ChangePasswordActivity::class.java))
                } else {
                    showToast("Password change is only available for email/password accounts")
                }
            }

            // Notifications section click handler
            notificationsLayout.setOnClickListener {
                notificationSwitch.toggle()
            }
        }
    }

    private fun handleNotificationToggle(enabled: Boolean) {
        // Save notification preference
        sharedPreferences.edit()
            .putBoolean("notifications_enabled", enabled)
            .apply()

        // Update Firebase messaging topic subscription
        updateNotificationSubscription(enabled)

        // Show feedback to user
        showToast(if (enabled) "Notifications enabled" else "Notifications disabled")
    }

    private fun updateNotificationSubscription(enabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        // Here you would typically update your notification settings in Firebase
        // For example, updating a user's notification preferences in the database
        // and managing FCM topic subscriptions
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}
