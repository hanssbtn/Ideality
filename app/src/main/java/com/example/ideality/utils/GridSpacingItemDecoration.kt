package com.example.ideality.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        // Add left spacing except for first column
        outRect.left = if (column == 0) spacing else spacing / 2
        // Add right spacing except for last column
        outRect.right = if (column == spanCount - 1) spacing else spacing / 2

        // Add top spacing for all rows except first
        if (position >= spanCount) {
            outRect.top = spacing
        }

        // Add bottom spacing for all items
        outRect.bottom = spacing
    }
}