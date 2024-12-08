package com.example.ideality.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

fun FloatArray.setXY(idx: Int, x: Float, y: Float) {
    this[(idx * 2) + 0] = x
    this[(idx * 2) + 1] = y
}

fun FloatArray.setUV(idx: Int, u: Float, v: Float) = setXY(idx, u, v)
