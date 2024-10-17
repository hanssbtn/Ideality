package com.example.ideality

import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView

class DraggableListBehavior: CoordinatorLayout.Behavior<RecyclerView>() {
    private val tag = "DraggableHandleBehavior"
    private var handleFullyDragged: Boolean = false

    override fun onTouchEvent(
        parent: CoordinatorLayout,
        child: RecyclerView,
        ev: MotionEvent
    ): Boolean {
        return child.onTouchEvent(ev)
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: RecyclerView,
        dependency: View
    ): Boolean {
        Log.d(tag,"layoutDependsOn: R.id.handle == dependency.id && child.id == R.id.recents: ${R.id.handle == dependency.id} && ${child.id == R.id.recents}")
        return R.id.handle == dependency.id && child.id == R.id.recents
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: RecyclerView,
        ev: MotionEvent
    ): Boolean {
        return handleFullyDragged
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: RecyclerView,
        dependency: View
    ): Boolean {
        handleFullyDragged = dependency.y <= 0
        if (dependency.y >= parent.height - dependency.height - 50) {
            child.translationY = 1000f
            child.visibility = View.INVISIBLE
        } else {
            child.translationY = 0f
            child.visibility = View.VISIBLE
            child.y = dependency.y
            child.alpha = (parent.height - dependency.y)/(parent.height)
        }
        return super.onDependentViewChanged(parent, child, dependency)
    }
}
