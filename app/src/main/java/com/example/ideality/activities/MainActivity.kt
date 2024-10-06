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
import com.example.ideality.R
import com.google.ar.core.ArCoreApk
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var userRequestedInstall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        checkAndInstallARCore()

        setContentView(R.layout.activity_main)

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
        builder.setCancelable(false) // Prevent the dialog from being dismissed by clicking outside
        builder.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}