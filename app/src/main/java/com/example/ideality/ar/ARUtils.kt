package com.example.ideality.ar

import android.view.View
import com.google.android.filament.utils.FPI
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Float4
import com.google.android.filament.utils.Mat4
import com.google.android.filament.utils.dot
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Trackable
import dev.romainguy.kotlin.math.dot
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.math.toPosition
import kotlin.math.PI
import kotlin.math.sqrt

fun View.getRect(): ViewRectangle = ViewRectangle(left.toFloat(), top.toFloat(), width.toFloat(), height.toFloat())

data class ScreenPosition(val x: Float, val y: Float)

sealed class TouchEvent(val x: Float, val y: Float) {
    class Move(x: Float, y: Float) : TouchEvent(x, y)
    class Stop(x: Float, y: Float) : TouchEvent(x, y)
}

fun FloatArray.setXY(idx: Int, x: Float, y: Float) {
    this[(idx * 2) + 0] = x
    this[(idx * 2) + 1] = y
}

fun FloatArray.setUV(idx: Int, u: Float, v: Float) = setXY(idx, u, v)

fun FloatArray.setXYZ(idx: Int, x: Float, y: Float, z: Float) {
    this[(idx * 3) + 0] = x
    this[(idx * 3) + 1] = y
    this[(idx * 3) + 2] = z
}

fun FloatArray.toMat4() =
    Mat4(
        x = Float4(this[0], this[1], this[2], this[3]),
        y = Float4(this[4], this[5], this[6], this[7]),
        z = Float4(this[8], this[9], this[10], this[11]),
        w = Float4(this[12], this[13], this[14], this[15])
    )

fun Float3.normalize() = this.div(sqrt(dot(this, this)))

fun Mat4.toColumnsFloatArray(): FloatArray = floatArrayOf(
    x.x, x.y, x.z, x.w,
    y.x, y.y, y.z, y.w,
    z.x, z.y, z.z, z.w,
    w.x, w.y, w.z, w.w
)

fun Pose.distanceToPlane(cameraPose: Pose): Float {
    val normal = FloatArray(3).apply {
        // Get transformed Y axis of plane's coordinate system.
        getTransformedAxis(1, 1.0f, this, 0)
    }.toPosition()
    val position = this.position
    val cameraPosition = cameraPose.position
    // Compute dot product of plane's normal with vector from camera to plane center.

    return dot((cameraPosition - position), normal)
}

fun Trackable.isValidPlane(result: HitResult, cameraPose: Pose): Boolean {
    return ((this is Plane
            && this.isPoseInPolygon(result.getHitPose())
            && this.centerPose.distanceToPlane(cameraPose) > 0))
}

fun Trackable.isValidPoint() =
    this is Point && (this.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)

fun Double.toDegrees(): Double = this * 180.0 / PI

fun Float.toDegrees(): Float = this * 180f / FPI

fun Float.toRadians(): Float = (this * FPI) / 180f

fun distance(f1: Float3, f2: Float3): Float = sqrt(dot(f1, f2))

fun Float3.length() = distance(this, this)

data class ViewRectangle(val left: Float, val top: Float, val width: Float, val height: Float)

