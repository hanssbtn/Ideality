package com.example.ideality

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ar.core.ArCoreApk
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ModelNode
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var arSceneView: ARSceneView
    var modelNode : ModelNode? = null

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
            else -> {
                // Handle the case where ARCore is not installed or supported
                val builder = AlertDialog.Builder(this)
                builder.setTitle("ARCore not supported")
                builder.setMessage("This device does not have ARCore installed/supported.")
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    cleanup()
                    exitProcess(0) // Close the app
                }
                builder.setCancelable(false) // Prevent the dialog from being dismissed by clicking outside
                builder.show()
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
}