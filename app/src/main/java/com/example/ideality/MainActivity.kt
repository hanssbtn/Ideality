package com.example.ideality

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ar.core.ArCoreApk
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ModelNode
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var arSceneView: ARSceneView
    var modelNode : ModelNode? = null
    private var userRequestedInstall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        when (ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                // ARCore is installed and supported

            }
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                showAlertDialog("Error: ARCore not supported", "This device does not have ARCore support.") {
                    cleanup()
                    exitProcess(0)
                }
            }
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                // Handle the case where ARCore is not installed or supported
                requestARCoreInstall()
            }
            else -> {}
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
        arSceneView.destroy()
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
}