package com.example.ideality.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.example.ideality.databinding.ActivityArViewerBinding
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch

class ARViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArViewerBinding
    private lateinit var sceneView: ARSceneView
    private lateinit var loadingView: View
    private lateinit var instructionText: TextView

    private var currentModelNode: ModelNode? = null
    private var initialScale = 2.0f

    private var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    private var anchorNode: AnchorNode? = null
        set(value) {
            if (field != value) {
                field = value
                updateInstructions()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val modelUrl = intent.getStringExtra("modelUrl") ?: run {
            showToast("No model URL provided")
            return finish()
        }

        setupViews()
        setupAR()
        setupButtons()
    }

    private fun setupViews() {
        loadingView = binding.loadingOverlay
        instructionText = binding.instructionText
        sceneView = binding.sceneView
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener { finish() }
        binding.resetButton.setOnClickListener { resetModel() }
    }

    private fun setupAR() {
        sceneView.apply {
            lifecycle = this@ARViewerActivity.lifecycle
            cameraStream = ARCameraStream(this.materialLoader)
            configureSession { session, config ->
                val depthSupported = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> {
                        config.depthMode = Config.DepthMode.AUTOMATIC
                        true
                    }
                    false -> {
                        config.depthMode = Config.DepthMode.DISABLED
                        false
                    }
                }
                cameraStream?.isDepthOcclusionEnabled = depthSupported
                Toast.makeText(this@ARViewerActivity, "setupAR.ARSceneView.configureSession: depth occlusion enabled: ${cameraStream?.isDepthOcclusionEnabled == true}", Toast.LENGTH_SHORT).show()
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            }

            onSessionUpdated = { session, frame ->
                if (anchorNode == null) {
                    frame.getUpdatedPlanes()
                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                        ?.let { plane ->
                            addAnchorNode(plane.createAnchor(plane.centerPose))
                        }
                }
            }
        }
    }

    private fun addAnchorNode(anchor: Anchor) {
        sceneView.addChildNode(
            AnchorNode(sceneView.engine, anchor).apply {
                isEditable = true
                lifecycleScope.launch {
                    isLoading = true
                    val modelUrl = intent.getStringExtra("modelUrl")
                    sceneView.modelLoader.loadModelInstance(modelUrl!!)?.let { modelInstance ->
                        val modelNode = ModelNode(
                            modelInstance = modelInstance,
                            scaleToUnits = initialScale,
                            centerOrigin = Position(y = 0f)
                        ).apply {
                            isEditable = true
                        }

                        addChildNode(modelNode)
                        currentModelNode = modelNode
                    }
                    isLoading = false
                }
                anchorNode = this
            }
        )
    }

    private fun resetModel() {
        currentModelNode?.let { node ->
            // Reset scale to initial value
            node.scale = Position(initialScale, initialScale, initialScale)

            // Reset rotation to default
            node.rotation = Position(0f, 0f, 0f)

            // Reset position relative to anchor
            node.position = Position(0f, 0f, 0f)

            showToast("Model position reset")
        }
    }

    private fun updateInstructions() {
        instructionText.text = if (anchorNode == null) {
            "Point your phone at a surface to place the model"
        } else {
            null
        }
    }

    private fun showToast(message: String) {
        instructionText.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}