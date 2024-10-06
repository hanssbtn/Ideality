package com.example.ideality.utils

import android.os.Handler
import android.os.Looper

abstract class ExponentialBackoffHandler (
    private var maxAttempts: Int = 0,
    private var base: Long = 2,
    private var delayDuration: Long = 5000L,
) {

    private var currentCount: Int = 0
    init {
        if (maxAttempts < 0 || maxAttempts > 10000) throw IllegalArgumentException("maxAttempts must be in range [0, 10000].")
        if (base < 0 || base > 10) throw IllegalArgumentException("base must be in range [0, 10].")
        if (delayDuration < 0) throw IllegalArgumentException("delayDuration cannot be < 0.>")
    }

    abstract fun action(): Boolean

    open fun onFailure() {

    }

    fun execute() {
        if (maxAttempts != 0 && currentCount == maxAttempts) {
            onFailure()
        }
        val d: Long = delayDuration * base
        val currentDelay: Long = if (d >= 3000000L || delayDuration >= 3000000L) {
            3000000L
        } else {
            d
        }
        currentCount++
        Handler(Looper.getMainLooper()).postDelayed({
            // Your backoff logic here
            action()
        }, currentDelay)
    }
}