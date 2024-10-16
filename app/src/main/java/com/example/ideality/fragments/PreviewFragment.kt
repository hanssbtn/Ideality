package com.example.ideality.fragments

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ideality.R
import com.example.ideality.utils.ModelPreviewListAdapter
import com.example.ideality.utils.ProductDataRepository
import com.example.ideality.viewmodels.PreviewListElementViewModel
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import com.google.ar.core.Config
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.node.Node
import kotlinx.coroutines.launch


class PreviewFragment: Fragment() {
    private val tag = "PreviewFragment"
    private lateinit var arSceneView: ARSceneView
    private lateinit var elementViewModel: PreviewListElementViewModel
    private lateinit var modelList: RecyclerView
//    private var modelNodes : ArrayMap<, ModelNode> = HashMap()
    private lateinit var modelLoader: ModelLoader
    private lateinit var loadingScreen: PopupWindow
    private lateinit var listAdapter: ModelPreviewListAdapter
    private var trackingFailureReason: TrackingFailureReason? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_preview, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tag = this.tag

        showLoadingScreen(checkCameraAvailability())
        arSceneView = view.findViewById(R.id.arSceneView)
        modelList = view.findViewById(R.id.model_list)
        modelList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        if (arSceneView.session == null) {
            Log.e(tag, "OnViewCreated: ARSceneView has no session")
        }

        modelLoader = ModelLoader(arSceneView.engine, view.context)
        arSceneView.apply {
            lifecycle = this@PreviewFragment.lifecycle
            planeRenderer.isEnabled = true

            configureSession { session, config ->
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                Log.d(tag, "arSceneView.apply.configureSession: session configured.")
            }

            onSessionUpdated = { session, frame ->
                val nodes = this.childNodes
                nodes.forEachIndexed { index: Int , node: Node ->
                    Log.d(tag, "arSceneView.apply.onSessionUpdated: node ${node.name} at index $index")
                    if (node is ModelNode) {
                        Log.d(tag, "arSceneView.apply.onSessionUpdated: Model Node found")
                    }
                }
            }

            onTrackingFailureChanged = { reason ->
                Log.d(tag, "arSceneView.apply.onTrackingFailureChanged: " + reason?.name)
                this@PreviewFragment.trackingFailureReason = reason
            }
        }
        lifecycleScope.launch {
            Log.d(tag, "lifeCycleScope.launch: loading model")
            val model = modelLoader.createModel("Lobster_Chair_N210823.glb")

            val modelInstance = model.instance
            Log.d(tag, "lifeCycleScope.launch.onViewCreated: created model instance.")

            val modelNode = ModelNode(modelInstance)
            modelNode.name = "chair"
            Log.d(tag, "lifeCycleScope.launch.onViewCreated: created model node.")

            arSceneView.addChildNode(modelNode)
            Log.d(tag, "lifeCycleScope.launch.onViewCreated: added child node.")

        }
        val repo = ProductDataRepository(requireContext())
        elementViewModel = PreviewListElementViewModel(repo)
        listAdapter = ModelPreviewListAdapter()
    }

    override fun onPause() {
        super.onPause()
        arSceneView.session?.pause()
        Log.d(tag, "onPause: Fragment paused.")
    }

    override fun onResume() {
        super.onResume()
        arSceneView.session?.resume()
        Log.d(tag, "onResume: Fragment resumed.")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(tag, "onAttach: Fragment attached.")
        val tmp = FrameLayout(context)
        val loadingScreenLayout = LayoutInflater.from(context).inflate(R.layout.loading_screen, tmp, false)
        loadingScreen = PopupWindow(loadingScreenLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
    }

    override fun onDestroy() {
        Log.d(tag, "onDestroy: Fragment destroyed.")
        super.onDestroy()
        cleanup()
    }

    override fun onDestroyView() {
        Log.d(tag, "onDestroyView: Fragment view destroyed.")
        super.onDestroyView()
        showLoadingScreen(false)
    }

    private fun cleanup() {
        arSceneView.session?.close()
        arSceneView.destroy()
    }

    private fun checkCameraAvailability(): Boolean {
        val cameraAvailable: Boolean = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        Log.d(tag, "checkCameraAvailability: camera is ${if (cameraAvailable) "" else "un"}available.")
        return cameraAvailable
    }

    private fun showLoadingScreen(condition: Boolean) {
        if (!loadingScreen.isShowing && condition) {
            Log.d(tag, "showLoadingScreen: Showing loading screen.")
            loadingScreen.showAtLocation(this.view, Gravity.CENTER, 0,0)
        }
        if (loadingScreen.isShowing && !condition) {
            Log.d(tag, "showLoadingScreen: Dismissing loading screen.")
            loadingScreen.dismiss()
        }
    }
}