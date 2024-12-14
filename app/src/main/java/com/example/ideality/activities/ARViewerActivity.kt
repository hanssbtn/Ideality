package com.example.ideality.activities

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.example.ideality.R
import com.example.ideality.databinding.ActivityArViewerBinding
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ARViewerActivity : AppCompatActivity() {
    companion object {
        const val TAG = "ARViewerActivity"
    }
    private lateinit var binding: ActivityArViewerBinding
    private lateinit var sceneView: ARSceneView
    private lateinit var instructionText: TextView

    private var currentModelNode: ModelNode? = null
    private var initialScale = 2.0f

    private var modelInstance: ModelInstance? = null

    private var anchorNode: AnchorNode? = null
        set(value) {
            if (field != value) {
                field = value
                displayStatus()
            }
        }
    private var modelUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupButtons()
        Log.d(TAG, "Loading model...")
        intent.getStringExtra("modelUrl").let {
            if (it == null) {
                Toast.makeText(this, "No model URL provided", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error: No model URL provided.")
                return finish()
            }
            modelUrl = it
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                runBlocking {
                    modelInstance = binding.sceneView.modelLoader.loadModelInstance(modelUrl)
                }
                Log.d(TAG, "Finished loading model.")
            }
            displayStatus()
        }.invokeOnCompletion { t ->
            if (t is Exception) {
                Toast.makeText(this, "Got unexpected error.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Got unexpected error.", t)
                return@invokeOnCompletion finish()
            }
            binding.loadingView.isGone = true
        }
    }

    private fun setupViews() {
        instructionText = binding.instructionText
        displayStatus()
        sceneView = binding.sceneView.apply {
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
                            if (modelInstance == null) return@let
                            addAnchorNode(plane.createAnchor(plane.centerPose))
                        }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener { finish() }
        binding.resetButton.setOnClickListener { resetModel() }
    }

    private fun addAnchorNode(anchor: Anchor) {
        displayStatus()
        lifecycleScope.launch {
            sceneView.addChildNode(
                AnchorNode(sceneView.engine, anchor).apply {
                    if (modelInstance == null) {
                        Log.e(TAG, "addAnchorNode: modelInstance is destroyed.")
                    }
                    modelInstance!!.let {
                        val modelNode = ModelNode(
                            modelInstance = it,
                            scaleToUnits = initialScale,
                            centerOrigin = Position(y = 0f)
                        ).apply {
                            isEditable = true
                        }

                        addChildNode(modelNode)
                        currentModelNode = modelNode
                    }
                    Log.d(TAG, "finished adding model")
                    isEditable = true
                    anchorNode = this
                }
            )
        }.invokeOnCompletion { t ->
            if (t == null) {
                Log.d(TAG, "Finished loading model.")
                return@invokeOnCompletion
            }
            if (t is Exception) {
                Log.d(TAG,"Got error", t)
                runCatching {
                    if ((anchorNode == null || anchorNode!!.anchor.trackingState != TrackingState.TRACKING)) {
                        resetModel()
                    }
                }
            }
        }
    }

    private fun resetModel() {
        try {
            anchorNode?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "resetModel: Failed to destroy anchor node", e)
        }
        anchorNode = null
        try {
            currentModelNode?.destroy()
        } catch (e: Exception) {
            Log.e(TAG,"resetModel: Failed to destroy currentModelNode", e)
        }
        currentModelNode = null
    }

    private fun displayStatus() {
        instructionText.text = if (modelInstance == null) {
            getString(R.string.loading_model)
        } else if (anchorNode == null) {
            getString(R.string.user_instructions)
        } else {
            ""
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            if ((anchorNode == null || anchorNode!!.anchor.trackingState != TrackingState.TRACKING)) {
                resetModel()
            }
        }
    }

    override fun onDestroy() {
        resetModel()
        super.onDestroy()
    }
}