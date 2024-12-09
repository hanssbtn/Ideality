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

        intent.getStringExtra("modelUrl") ?: run {
            Toast.makeText(this, "No model URL provided", Toast.LENGTH_SHORT).show()
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

            configureSession { session, config ->
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            }

            onSessionUpdated = { _, frame ->
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
        runCatching {
            anchorNode?.destroy()
            anchorNode = null
        }
    }

    private fun updateInstructions() {
        instructionText.text = if (anchorNode == null) {
            "Point your phone at a surface to place the model"
        } else {
            ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}