package com.example.ideality.ar.models

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.ideality.ar.ARCoreObject
import com.example.ideality.ar.ScreenPosition
import com.example.ideality.ar.toDegrees
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Mat4
import com.google.android.filament.utils.TWO_PI
import com.google.android.filament.utils.rotation
import com.google.android.filament.utils.scale
import com.google.android.filament.utils.translation
import com.google.ar.core.Frame
import com.google.ar.core.Point
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor

class PreviewScreenRenderer(
    val context: Context,
    val arCoreObject: ARCoreObject
) {
    companion object {
        const val TAG = "PreviewScreenRenderer"
    }

    sealed class ModelEvent {
        data class Move(val screenPosition: ScreenPosition) : ModelEvent()
        data class Update(val rotate: Float, val scale: Float) : ModelEvent()
    }

    val modelEvents: MutableSharedFlow<ModelEvent> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val doFrameEvents: MutableSharedFlow<Frame> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val canDrawBehavior: MutableStateFlow<Unit?> =
        MutableStateFlow(null)

    val assets = HashMap<String, FilamentAsset>()
    val instances = HashMap<Int, PreviewModel>()

    val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var translate: Float3 = Float3()
    private var rotate: Float = 0f
    private var scale: Float = 0.25f

    fun init(objFile: String) {
        if (assets.containsKey(objFile)) return
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    //TODO Change to Appwrite stored file.
                    context.assets.open(objFile).use { stream ->
                        ByteArray(stream.available()).let { arr ->
                            stream.read(arr)
                            arCoreObject.filament.assetLoader.createAsset(ByteBuffer.wrap(arr))!!
                        }
                    }
                } catch (ioe: IOException) {
                    Log.e(TAG, "Failed to open file ${Uri.parse(objFile)}")
                    null
                }?.also {
                    arCoreObject.filament.resourceLoader.loadResources(it)
                    assets[objFile] = it
                    val previewModel = PreviewModel(it.instance)
                    instances[previewModel.entity] = previewModel
                }
            }

            launch {
                // translation
                modelEvents
                    .mapNotNull { modelEvent ->
                        (modelEvent as? ModelEvent.Move)
                            ?.let {
                                arCoreObject.frame
                                    .hitTest(
                                        arCoreObject.surfaceView.width.toFloat() * modelEvent.screenPosition.x,
                                        arCoreObject.surfaceView.height.toFloat() * modelEvent.screenPosition.y,
                                    )
                                    .maxByOrNull { it.trackable is Point }
                            }
                            ?.let {
                                val tr = it.hitPose.translation
                                Float3(tr[0], tr[1], tr[2])
                            }
                    }
                    .collect {
                        canDrawBehavior.tryEmit(Unit)
                        translate = it
                    }
            }

            launch {
                // rotation and scale
                modelEvents.collect { modelEvent ->
                    when (modelEvent) {
                        is ModelEvent.Update ->
                            Pair((rotate + modelEvent.rotate).let {
                                when {
                                    it < 0f ->
                                        it + ceil(-it / TWO_PI) * TWO_PI

                                    it >= TWO_PI ->
                                        it - floor(it / TWO_PI) * TWO_PI

                                    else ->
                                        it
                                }
                            }, scale * modelEvent.scale)
                        else ->
                            Pair(rotate, scale)
                    }
                        .let { (r, s) ->
                            rotate = r
                            scale = s
                        }
                }
            }

            launch {
                canDrawBehavior.filterNotNull().first()

                doFrameEvents.collect { frame ->
                    // update animator
                    val animator = assets[objFile]!!.instance.animator

                    if (animator.animationCount > 0) {
                        animator.applyAnimation(
                            0,
                            (frame.timestamp /
                                    TimeUnit.SECONDS.toNanos(1).toDouble())
                                .toFloat() %
                                    animator.getAnimationDuration(0),
                        )

                        animator.updateBoneMatrices()
                    }

                    arCoreObject.filament.scene.addEntities(assets[objFile]!!.entities)

                    arCoreObject.filament.engine.transformManager.setTransform(
                        arCoreObject.filament.engine.transformManager.getInstance(assets[objFile]!!.root),
                        Mat4.identity()
                            .times(translation(translate))
                            .times(rotation(Float3(0f, 1f, 0f), rotate.toDegrees()))
                            .times(scale(Float3(scale)))
                            .toFloatArray(),
                    )
                }
            }
        }
    }

    fun destroy() {
        coroutineScope.launch(CoroutineExceptionHandler { cctx, th ->
            Log.e(TAG,"destroy: Failed to destroy asset.", th)
        }) {
            instances.forEach { (k, model) ->
                model.materialAsset.entities.forEach { entity ->
                    arCoreObject.filament.engine.destroyEntity(entity)
                    arCoreObject.filament.engine.entityManager.destroy(entity)
                }
            }
            assets.forEach { (k, v) ->
                v.releaseSourceData()
                arCoreObject.filament.assetLoader.destroyAsset(v)
            }
        }
        coroutineScope.cancel()
    }

    fun doFrame(frame: Frame) {
        doFrameEvents.tryEmit(frame)
    }
}