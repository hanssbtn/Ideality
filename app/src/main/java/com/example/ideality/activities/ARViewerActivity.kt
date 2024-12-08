package com.example.ideality.activities

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.example.ideality.databinding.ActivityArViewerBinding
import com.example.ideality.utils.setUV
import com.example.ideality.utils.setXY
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.utils.Mat4
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.channels.Channels

class ARViewerActivity : AppCompatActivity() {
    companion object {
        const val NEAR = 0.1f
        const val FAR = 30.0f
        const val NEAR_DOUBLE = 0.1
        const val FAR_DOUBLE = 30.0
        private const val POSITION_BUFFER_INDEX: Int = 0
        private const val UV_BUFFER_INDEX: Int = 1
    }

    private lateinit var binding: ActivityArViewerBinding
    private lateinit var sceneView: ARSceneView
    private lateinit var loadingView: View
    private lateinit var instructionText: TextView
    private var entity = -1
    private lateinit var texture: Texture
    private lateinit var session: Session
    private lateinit var frame: Frame
    private lateinit var cameraStream: ARCameraStream

    private var currentModelNode: ModelNode? = null
    private var initialScale = 2.0f

    private var modelUrl: String = ""

    private val cameraManager: CameraManager =
        ContextCompat.getSystemService(this, CameraManager::class.java)!!

    private var currentCameraRotation = 0

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

        modelUrl = intent.getStringExtra("modelUrl") ?: run {
            Toast.makeText(this, "Model position reset", Toast.LENGTH_SHORT).show()
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
            ARCameraStream(this.materialLoader, depthOcclusionMaterialFile = "materials/depth.filamat").let {
                this@ARViewerActivity.cameraStream = it
                cameraStream = it
            }
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
                val tesselation = run {
                    val tesWidth = 1
                    val tesHeight = 1

                    val clipPosition =
                        FloatArray((((tesWidth * tesHeight) + tesWidth + tesHeight + 1) * 2))

                    val uvs =
                        FloatArray((((tesWidth * tesHeight) + tesWidth + tesHeight + 1) * 2))

                    for (k in 0..tesHeight) {
                        val v = k.toFloat() / tesHeight.toFloat()
                        val y = (k.toFloat() / tesHeight.toFloat()) * 2f - 1f

                        for (i in 0..tesWidth) {
                            val u = i.toFloat() / tesWidth.toFloat()
                            val x = (i.toFloat() / tesWidth.toFloat()) * 2f - 1f
                            clipPosition.setXY(k * (tesWidth + 1) + i, x, y)
                            uvs.setUV(k * (tesWidth + 1) + i, u, v)
                        }
                    }

                    val triangleIndices = ShortArray(tesWidth * tesHeight * 6)

                    for (k in 0 until tesHeight) {
                        for (i in 0 until tesWidth) {
                            triangleIndices[((k * tesWidth + i) * 6) + 0] =
                                ((k * (tesWidth + 1)) + i + 0).toShort()
                            triangleIndices[((k * tesWidth + i) * 6) + 1] =
                                ((k * (tesWidth + 1)) + i + 1).toShort()
                            triangleIndices[((k * tesWidth + i) * 6) + 2] =
                                ((k + 1) * (tesWidth + 1) + i).toShort()

                            triangleIndices[((k * tesWidth + i) * 6) + 3] =
                                ((k + 1) * (tesWidth + 1) + i).toShort()
                            triangleIndices[((k * tesWidth + i) * 6) + 4] =
                                ((k * (tesWidth + 1)) + i + 1).toShort()
                            triangleIndices[((k * tesWidth + i) * 6) + 5] =
                                ((k + 1) * (tesWidth + 1) + i + 1).toShort()
                        }
                    }

                    return@run Triple(clipPosition, uvs, triangleIndices)
                }

                this@ARViewerActivity.frame = frame
                try {
                    frame.acquireDepthImage16Bits().let { depthImage ->
                        val depthMaterialInstance = assets.openFd("materials/depth.filamat")
                            .use { fd ->
                                val input = fd.createInputStream()
                                val dst = ByteBuffer.allocate(fd.length.toInt())

                                val src = Channels.newChannel(input)
                                src.read(dst)
                                src.close()

                                dst.apply { rewind() }
                            }.let { byteBuffer ->
                                Material
                                    .Builder()
                                    .payload(byteBuffer, byteBuffer.remaining())
                            }
                            .build(engine)
                            .createInstance()
                            .also { materialInstance ->
                                materialInstance.setParameter(
                                    /* name = */ "depthTexture",
                                    /* texture = */
                                    Texture
                                        .Builder()
                                        .width(depthImage.width)
                                        .height(depthImage.height)
                                        .sampler(Texture.Sampler.SAMPLER_2D)
                                        .format(Texture.InternalFormat.RG8)
                                        .levels(1)
                                        .build(engine)
                                        .also { texture = it },
                                    /* sampler = */ TextureSampler(), //.also { it.anisotropy = 8.0f }
                                )
                                materialInstance.setParameter(
                                    "uvTransform",
                                    MaterialInstance.FloatElement.FLOAT4 /* type = */ ,
                                    floatArrayOf(
                                        1f,0f,0f,0f,
                                        0f,1f,0f,0f,
                                        0f,0f,1f,0f,
                                        0f,0f,0f,1f),
                                    /* offset = */ 0,
                                    /* count = */ 4,
                                )
                            }

                        RenderableManager
                            .Builder(1)
                            .castShadows(false)
                            .receiveShadows(false)
                            .culling(false)
                            .geometry(
                                0,
                                PrimitiveType.TRIANGLES,
                                VertexBuffer
                                    .Builder()
                                    .vertexCount(tesselation.first.count())
                                    .bufferCount(2)
                                    .attribute(
                                        /* attribute = */ VertexAttribute.POSITION,
                                        /* bufferIndex = */ POSITION_BUFFER_INDEX,
                                        /* attributeType = */ AttributeType.FLOAT2,
                                        /* byteOffset = */ 0,
                                        /* byteStride = */ 0,
                                    )
                                    .attribute(
                                        /* attribute = */ VertexAttribute.UV0,
                                        /* bufferIndex = */ UV_BUFFER_INDEX,
                                        /* attributeType = */ AttributeType.FLOAT2,
                                        /* byteOffset = */ 0,
                                        /* byteStride = */ 0,
                                    )
                                    .build(engine)
                                    .also { vertexBuffer ->
                                        vertexBuffer.setBufferAt(
                                            /* engine = */ engine,
                                            /* bufferIndex = */ POSITION_BUFFER_INDEX,
                                            /* buffer = */ FloatBuffer.wrap(tesselation.first)
                                        )

                                        vertexBuffer.setBufferAt(
                                            /* engine = */ engine,
                                            /* bufferIndex = */ UV_BUFFER_INDEX,
                                            /* buffer = */ FloatBuffer.wrap(tesselation.second)
                                        )
                                    },
                                IndexBuffer
                                    .Builder()
                                    .indexCount(tesselation.third.size)
                                    .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                                    .build(engine)
                                    .apply { setBuffer(engine, ShortBuffer.wrap(tesselation.third)) },
                            )
                            .material(0, depthMaterialInstance)
                            .build(engine, EntityManager.get().create().also {
                                entity = it
                                scene.addEntity(it)
                            })

                        texture.setImage(
                            engine,
                            0,
                            Texture.PixelBufferDescriptor(
                                depthImage.planes[0].buffer,
                                Texture.Format.RG,
                                Texture.Type.UBYTE,
                                1,
                                0,
                                0,
                                0,
                                @Suppress("DEPRECATION")
                                Handler(),
                            ) {
                                depthImage.close()
                            }
                        )

                        depthMaterialInstance.setParameter(
                            "uvTransform",
                            MaterialInstance.FloatElement.FLOAT4,
                            FloatArray(16).also {
                                Matrix.setIdentityM(it, 0)
                                Matrix.translateM(it, 0, 0.5f, 0.5f, 0f)
                                Matrix.rotateM(it, 0, imageRotation().toFloat(), 0f, 0f, -1f)
                                Matrix.translateM(it, 0, -.5f, -.5f, 0f)
                            },
                            0,
                            4,
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ARViewerActivity", "setupAR: got error ", e)
                }


                if (anchorNode == null) {
                    frame.getUpdatedPlanes()
                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                        ?.let { plane ->
                            addAnchorNode(plane.createAnchor(plane.centerPose))
                        }
                }
                if (this@ARViewerActivity::texture.isInitialized) {
                    onSessionUpdated = { session, frame ->
                        this@ARViewerActivity.frame = frame
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

            onFrame = { frameTimeNanos ->
                try {
                    frame!!.acquireDepthImage16Bits().use { depthImage ->

                    }

                } catch (e: Exception) {
                    Log.e("ARViewerActivity", "setupAR.onFrame: got error ", e)
                }

            }

            onSessionCreated = { session ->
                this@ARViewerActivity.session = session
            }

            onTrackingFailureChanged = { reason ->
                if (reason != null) {
                    instructionText.text = "ERR: ${reason.name}"
                } else {
                    updateInstructions()
                }
            }
        }
    }

    private fun addAnchorNode(anchor: Anchor) {
        sceneView.addChildNode(
            AnchorNode(sceneView.engine, anchor).also {
                it.isEditable = true
                lifecycleScope.launch(
                    CoroutineExceptionHandler { _, t ->
                        Log.e("ARViewerActivity", "addAnchorNode: Got unexpected error ", t)
                    }
                ) {
                    isLoading = true
                    sceneView.modelLoader.loadModelInstance(modelUrl)?.let { modelInstance ->
                        val modelNode = ModelNode(
                            modelInstance = modelInstance,
                            scaleToUnits = initialScale,
                            centerOrigin = Position(y = 0f)
                        ).apply {
                            isEditable = true
                        }

                        it.addChildNode(modelNode)
                        currentModelNode = modelNode
                    }
                    isLoading = false
                }
                anchorNode = it
            }
        )
    }

    private fun imageRotation(): Int = (cameraManager
        .getCameraCharacteristics(session.cameraConfig.cameraId)
        .get(CameraCharacteristics.SENSOR_ORIENTATION)!! +
            when (currentCameraRotation) {
                0 -> 90
                90 -> 0
                180 -> 270
                270 -> 180
                else -> throw Exception()
            } + 270) % 360

    private fun resetModel() {
        currentModelNode?.let { node ->
            // Reset scale to initial value
            node.scale = Scale(initialScale, initialScale, initialScale)

            // Reset rotation to default
            node.rotation = Rotation(0f, 0f, 0f)

            // Reset position relative to anchor
            node.position = Position(0f, 0f, 0f)

            Toast.makeText(this, "Model position reset", Toast.LENGTH_SHORT).show()
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
        runCatching {
            EntityManager.get().destroy(entity)
        }
        super.onDestroy()
    }
}