package com.example.ideality.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.ideality.R
import com.google.ar.core.ArCoreApk
import kotlin.system.exitProcess
import com.example.ideality.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var userRequestedInstall = true

    private lateinit var appURL: String
    private lateinit var EMAIL: String
    private lateinit var PASSWORD: String

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
//        val splashScreen =
        installSplashScreen()
        super.onCreate(savedInstanceState)

        appURL = "http://192.168.18.40/api/logIn.php"

        checkAndInstallARCore()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Call the login function wherever needed, e.g., when a login button is clicked

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
    }

    private fun checkAndInstallARCore() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            Handler(Looper.getMainLooper()).postDelayed({
                checkAndInstallARCore()
            }, 2000)
        } else {
            when (ArCoreApk.getInstance().checkAvailability(this)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    // ARCore is installed and supported
                    return
                }
                ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Error: Failed to check device compatibility (timed out)")
                    builder.setMessage("Would you like to try again?")
                    builder.setPositiveButton("Yes") { dialog, _ ->
                        dialog.dismiss()
                        userRequestedInstall = true
                        requestARCoreInstall()
                    }
                    builder.setPositiveButton("No") { dialog, _ ->
                        dialog.dismiss()
                        cleanup()
                        exitProcess(1)
                    }
                }

                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    // Handle the case where ARCore is not installed or supported
                    requestARCoreInstall()
                }
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    showAlertDialog("Error: ARCore not supported",
                        "This device does not have ARCore support.") {
                        cleanup()
                        exitProcess(0)
                    }
                }
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                    showAlertDialog("Error: Incompatible application version",
                        "The current version of the app is no longer supported.") {
                        cleanup()
                        exitProcess(0) // Close the app
                    }
                }
                ArCoreApk.Availability.UNKNOWN_ERROR -> {
                    showAlertDialog("Error", "An unknown error occured.") {
                        cleanup()
                        exitProcess(0)
                    }
                }
                ArCoreApk.Availability.UNKNOWN_CHECKING -> {}
            }
        }

    }

    private fun requestARCoreInstall() {
        try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // ARCore is installed, proceed with AR functionality
                    return
                }
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // ARCore installation requested, pause the app until installation completes
                    userRequestedInstall = false
                }
            }
        } catch (e: Exception) {
            showAlertDialog(
                "Error: ARCore installation failed",
                "ARCore installation was not successful. Please try again."
            ) {
                cleanup()
                exitProcess(0) // Close the app
            }
        }

    }

    private fun cleanup() {

    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun showAlertDialog(title: String, message: String, onPositiveClick: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onPositiveClick()
        }
        builder.setCancelable(false)
        builder.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }


    private fun logIn() {

        if (EMAIL.isEmpty()) {
            showAlert("Email cannot be empty")
        } else if (PASSWORD.isEmpty()) {
            showAlert("Password cannot be empty")
        } else if (PASSWORD.length < 8) {
            showAlert("Password length must be at least 8 characters")
        } else {
            val stringRequest = object : StringRequest(Request.Method.POST, appURL,
                Response.Listener<String> { response ->
                    // Handle the server response here
                    if (response == "true") {
                        // Navigate to HomeFragment
                        val navController = findNavController(R.id.nav_host_fragment_activity_main)
                        navController.navigate(R.id.homeFragment)
                    } else {
                        showAlert(response)
                    }
                },
                Response.ErrorListener { error ->
                    // Handle error response
                    val errorMessage = if (error.networkResponse != null) {
                        String(error.networkResponse.data)
                    } else {
                        "Network error. Please try again."
                    }
                    showAlert(errorMessage)
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    return mapOf("Accept" to "application/json; charset=UTF-8")
                }

                @Throws(AuthFailureError::class)
                override fun getParams(): Map<String, String> {
                    return mapOf(
                        "email" to EMAIL,
                        "password" to PASSWORD
                    )
                }
            }

            // Add the request to the request queue
            Volley.newRequestQueue(this).add(stringRequest)
        }
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}



