package com.example.ideality.ar.models

import com.google.android.filament.Entity
import com.google.android.filament.gltfio.FilamentInstance
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Mat4
import com.google.android.filament.utils.Quaternion
import com.google.android.filament.utils.rotation
import com.example.ideality.ar.toMat4
import com.example.ideality.ar.toColumnsFloatArray
import com.google.android.filament.utils.scale
import com.google.android.filament.utils.translation

class PreviewModel(
    val filamentInstance: FilamentInstance,
    @Entity val entity: Int = filamentInstance.asset.root
) {
    val transformManager = filamentInstance.asset.engine.transformManager

    val materialAsset = filamentInstance.asset

    var transform: Mat4
        get() = FloatArray(16).let {
            transformManager.getTransform(transformManager.getInstance(entity), it)
        }.toMat4()
        set(value) {
            transformManager.setTransform(transformManager.getInstance(entity), value.toColumnsFloatArray())
        }

    var pos: Float3
        get() = transform.position
        set(value) {
            transform = transform(value, quaternion, scale)
        }

    var scale: Float3
        get() = transform.scale
        set(value) {
            transform = transform(pos, quaternion, value)
        }

    var quaternion: Quaternion
        get() = transform.let {
            rotation(it).toQuaternion()
        }
        set(value) {
            transform = transform(pos, value, scale)
        }

    var worldTransform: Mat4
        get() = FloatArray(16).let{
            transformManager.getWorldTransform(transformManager.getInstance(entity), it)
        }.toMat4()
        set(value) {
            transform = value
        }

    fun transform(pos: Float3, quaternion: Quaternion, scale: Float3) =
        Mat4.identity().times(translation(pos)).times(rotation(quaternion)).times(scale(scale))
}