package com.example.ideality.ar

import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.exceptions.FatalException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.firebase.auth.ktx.auth
import com.example.ideality.ar.ARCoreObject
import com.example.ideality.ar.models.PreviewScreenRenderer
import com.example.ideality.ui.theme.AppTheme
import io.github.sceneview.gesture.RotateGestureDetector
import android.Manifest
import android.content.Intent
import androidx.compose.runtime.mutableFloatStateOf
import com.example.ideality.R
import com.example.ideality.gestures.TransformationSystem
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class ARActivity : AppCompatActivity() {
    companion object {
        const val TAG = "ARActivity"
        const val NORMAL_ARROW = "Pointing Arrow.glb"
        const val HELMET = "models/damaged_helmet.glb"
        const val DEPTH_MATERIAL = "materials/depth_material.filamat"
        const val MAX_ANCHORS = 20
    }

    enum class ARCoreInstallResult {
        INSTALLING,
        INSTALLED,
        CANCELLED,
        DEVICE_INCOMPATIBLE,
        SDK_TOO_OLD,
        APK_TOO_OLD,
        UNKNOWN_ERROR,
    }

    enum class ARMode {
        SELECTING,
        PLACING,
        ROTATING,
        REMOVING
    }

    private val resumeBehavior: MutableStateFlow<Unit?> =
        MutableStateFlow(null)

    private val configurationChangedEvents: MutableSharedFlow<Configuration> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val dragEvents: MutableSharedFlow<Pair<ViewRectangle, TouchEvent>> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val scaleEvents: MutableSharedFlow<Float> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val rotateEvents: MutableSharedFlow<Float> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val arTrackingEvents: MutableSharedFlow<Unit> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val arCoreBehavior: MutableStateFlow<Pair<ARCoreObject, FrameCallback>?> =
        MutableStateFlow(null)

    private lateinit var transformationSystem: TransformationSystem

    private lateinit var askCameraPermission: ActivityResultLauncher<String>
    private lateinit var appSettingsLauncher: ActivityResultLauncher<Intent>
    private var requestedCameraPermission = false
    private var appSettingsRequested = false
    private var userRequestedInstall = true
    private var session: Session? = null
    private lateinit var arCoreObject: ARCoreObject
    private val arMode = mutableStateOf<ARMode>(ARMode.PLACING)
    private val anchors = ArrayList<Anchor>()

    private var modelFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.intent.extras?.let {
            modelFilePath = it.getString("model_filepath")
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )
        askCameraPermission = this.activityResultRegistry.register(
            "camera_permission",
            ActivityResultContracts.RequestPermission()
        ) {
            granted ->
            if (!granted) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                    )
                ) {
                    appSettingsRequested = true
                    Toast.makeText(this, "Opening app settings", Toast.LENGTH_SHORT).show()
                    appSettingsLauncher.launch(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", this@ARActivity.packageName, null)
                    })
                }
            }
        }
        appSettingsLauncher = this.activityResultRegistry.register(
            "show_application_settings",
            ActivityResultContracts.StartActivityForResult()
        ) {
            appSettingsRequested = false
        }

        setContent {
            AppTheme {
                PreviewScreen()
            }
        }
    }

    fun requestCameraPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askCameraPermission.launch(Manifest.permission.CAMERA)
            false
        } else {
            true
        }
    }

    fun checkCameraPermission(): Boolean {
        if (!requestedCameraPermission && !requestCameraPermission()) {
            requestedCameraPermission = true
        } else if (!appSettingsRequested) {
            return true
        }
        return false
    }

    @Composable
    fun PreviewScreen() {
        arCoreObject.init(this)
        val renderer = remember { PreviewScreenRenderer(this, arCoreObject) }
        val gestureDetector = remember {
            object {
                val standardGestureDetector = GestureDetector(this@ARActivity, object : GestureDetector.SimpleOnGestureListener() {

                })
                val rotateGestureDetector = RotateGestureDetector(this@ARActivity, object : RotateGestureDetector.SimpleOnRotateListener {
                    override fun onRotate(
                        detector: RotateGestureDetector,
                        e: MotionEvent
                    ): Boolean {
                        return true
                    }

                    override fun onRotateBegin(
                        detector: RotateGestureDetector,
                        e: MotionEvent
                    ): Boolean {
                        return super.onRotateBegin(detector, e)
                    }

                    override fun onRotateEnd(detector: RotateGestureDetector, e: MotionEvent) {
                        super.onRotateEnd(detector, e)
                    }
                })

                fun onTouchEvent(motionEvent: MotionEvent): Boolean {
                    when (arMode.value) {
                        ARMode.PLACING,
                        ARMode.SELECTING -> {
                            standardGestureDetector.onTouchEvent(motionEvent)
                        }
                        ARMode.ROTATING -> {
                            rotateGestureDetector.onTouchEvent(motionEvent)
                        }
                        ARMode.REMOVING -> {

                        }
                    }
                    return true
                }
            }
        }
        val callback =
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { _ ->
                arCoreObject.surfaceView.apply {
                    setOnTouchListener { v, event ->

                        v.performClick()
                        true
                    }
                }

            }
        )
    }

    @Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun MainPreview() {
        val listState = rememberLazyListState()
        var isSelected by remember { mutableStateOf(false) }
        val transition = updateTransition(isSelected, "drag transition")
        var offset by remember { mutableFloatStateOf(0f) }
        val offsetTransition by transition.animateIntOffset(
            transitionSpec = {
                when (isSelected) {
                    true -> snap(0)
                    else -> tween(500)
                }
            },
            label = "offset transition"
        ) { selected ->
            if (selected) {
                IntOffset(0, offset.toInt())
            } else {
                IntOffset(0, (if (offset >= 30f) 60 else 0))
            }
        }
        AppTheme {
            ConstraintLayout(
                Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues())) {
                val (left, right, up, down) = createRefs()
                // Model list
                val (list, msg) = createRefs()
                Box(Modifier.fillMaxSize()) {

                }
                Text(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(0.75f)
                        .constrainAs(msg) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(list.top, margin = 50.dp)
                        }
                        .layout { measurable, constraints ->
                            val offsetValue = offsetTransition
                            val placeable = measurable.measure(constraints)
                            layout(
                                placeable.width + offsetValue.x,
                                placeable.height + offsetValue.y
                            ) {
                                placeable.placeRelative(offsetValue)
                            }
                        },
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    softWrap = true,
                    text = "Tracking"
                )
                Column(
                    modifier = Modifier
                        .constrainAs(list) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom, margin = 20.dp)
                        }
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .offset { offsetTransition },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    Box(
                        Modifier
                            .defaultMinSize(20.dp, 10.dp)
                            .fillMaxWidth(0.25f)
                            .height(10.dp)
                            .draggable(
                                state = rememberDraggableState { dpx ->
                                    val pos = offset + dpx
                                    offset = pos.coerceIn(0f, 300f)
                                },
                                orientation = Orientation.Vertical,
                                onDragStarted = { _ ->
                                    isSelected = true
                                },
                                onDragStopped = { _ ->
                                    isSelected = false
                                },
                            )
                            .background(Color.White, shape = RoundedCornerShape(50))
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .defaultMinSize(300.dp, 60.dp)
                            .border(color = Color.White, width = 2.dp)
                            .padding(0.dp)
                            .graphicsLayer(alpha = (offset / 60f)),
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(20) {
                            Box(
                                Modifier
                                    .size(80.dp, 80.dp)
                                    .background(Color.White)) {

                            }
                        }
                    }
                }
            }
        }
    }

    private fun getARCoreVersion(): String {
        val version =  try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L)).versionName
            else packageManager.getPackageInfo("com.google.ar.core", 0).versionName
        } catch (nnfe: NameNotFoundException) {
            null
        } ?: "Not found"
        return version
    }

    override fun onPause() {
        super.onPause()
        if (session != null) {
            session!!.pause()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        arCoreObject.configChange(this)

    }

    override fun onResume() {
        super.onResume()
        if (session != null) session!!.resume()
        else {
            when (val res = requestARCoreInstall()) {
                ARCoreInstallResult.INSTALLING -> {
                    userRequestedInstall = false
                    return
                }
                ARCoreInstallResult.INSTALLED -> {}
                else -> {
                    Log.e(TAG, "Failed to install (${res.name}). Closing program")
                    AlertDialog.Builder(this)
                        .setIcon(R.drawable.baseline_error_outline_24)
                        .setTitle("Error installing ARCore")
                        .setMessage( when(res) {
                            ARCoreInstallResult.CANCELLED -> "User cancelled"
                            ARCoreInstallResult.APK_TOO_OLD -> "Application not supported"
                            ARCoreInstallResult.SDK_TOO_OLD -> "ARCore APK not supported (version ${getARCoreVersion()})"
                            ARCoreInstallResult.DEVICE_INCOMPATIBLE -> "Device is incompatible"
                            ARCoreInstallResult.UNKNOWN_ERROR -> "Unknown Error"
                            else -> ""
                        })
                        .setCancelable(false)
                        .setNeutralButton("OK") { dialog, _ ->
                            Log.d(TAG, "Closing application")
                            dialog.dismiss()
                            this.finish()
                        }
                        .create()
                        .show()
                }
            }
            if (checkCameraPermission()) {
                try {
                    session = Session(this).also { session ->
                        session.config.apply {
                            depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                Log.d(ARCoreObject.TAG,"Depth mode: Automatic")
                                Config.DepthMode.AUTOMATIC
                            } else {
                                Log.d(ARCoreObject.TAG,"Depth mode: Disabled")
                                Config.DepthMode.DISABLED
                            }
                            focusMode = Config.FocusMode.AUTO
                            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                            textureUpdateMode = Config.TextureUpdateMode.BIND_TO_TEXTURE_EXTERNAL_OES
                            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                            instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                        }.let(session::configure)
                    }
                    arCoreObject = ARCoreObject(this, session = session!!)
                } catch (se: SecurityException) {
                    Log.e(TAG, "Does not have permission despite asking")
                }
            } else {
                // Close activity
                this.finish()
            }
        }
    }

    private fun cleanup() {
        arCoreObject.destroy()
        session?.close()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun requestARCoreInstall(): ARCoreInstallResult {
        try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // ARCore is installed, proceed with AR functionality
                    return ARCoreInstallResult.INSTALLED
                }

                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // ARCore installation requested, pause the app until installation completes
                    return ARCoreInstallResult.INSTALLING
                }
            }
        } catch (udie: UnavailableUserDeclinedInstallationException) {
            return ARCoreInstallResult.CANCELLED
        } catch (atoe: UnavailableApkTooOldException) {
            return ARCoreInstallResult.APK_TOO_OLD
        } catch (uste: UnavailableSdkTooOldException) {
            return ARCoreInstallResult.SDK_TOO_OLD
        } catch (dnce: UnavailableDeviceNotCompatibleException) {
            return ARCoreInstallResult.DEVICE_INCOMPATIBLE
        } catch (fe: FatalException) {
            return ARCoreInstallResult.UNKNOWN_ERROR
        }
    }
}
