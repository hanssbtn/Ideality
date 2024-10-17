package com.example.ideality.utils

import android.animation.Animator
import android.animation.TimeInterpolator
import android.graphics.Interpolator
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.util.AttributeSet

class DraggableHandleBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<RecyclerView>(context, attrs) {
    private val tag = "DraggableHandleBehavior"
    private val handleRect = Rect(0,0,0,0)

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: RecyclerView,
        ev: MotionEvent
    ): Boolean {
        when(ev.action) {
            MotionEvent.ACTION_DOWN -> {
                val mid = (child.width)/2 + child.x.toInt()
                val paddingHorizontal = 50
                val paddingVertical = 25
                handleRect.apply {
                    set(mid - paddingHorizontal, child.y.toInt() - paddingVertical,
                        mid + paddingHorizontal, child.y.toInt() + paddingVertical)
                }
                val handleTouched = handleRect.contains(ev.x.toInt(), ev.y.toInt())
                Log.d(tag, "onInterceptTouchEvent: handleTouched = ${handleTouched}")
                return handleTouched
            }
            else -> return false
        }
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: RecyclerView, ev: MotionEvent): Boolean {
        Log.d(tag, "onTouchEvent: got ev.y = ${ev.y} and child.y = ${child.y}")
        if (ev.y < 0 || ev.y > parent.height - 50) return false
        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                child.y =  ev.y
            }
            MotionEvent.ACTION_UP -> {
                var end = 0
                if (ev.y > parent.height / 2) {
                    end = parent.height - child.height
                }
                child.animate()
                    .y(end.toFloat())
                    .setInterpolator(DecelerateInterpolator())
                    .setDuration(250)
                    .start()
            }
        }
        return true
    }
}