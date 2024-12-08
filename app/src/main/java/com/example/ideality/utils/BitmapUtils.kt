package com.example.ideality.utils

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.ideality.R
import com.example.ideality.activities.Home
import com.example.ideality.activities.Home.Companion.TAG
import com.example.ideality.activities.MainActivity
import java.io.IOException

fun Home.getBitmapOrPlaceholder(
    filePath: String,
    placeholderId: Int = R.drawable.placeholder_sofa
): Bitmap {
    return try {
        with(assets.open(filePath)) {
            BitmapFactory.decodeStream(this)
        }
    } catch (ioe: IOException) {
        Log.e(TAG, "loadRecentlyUsedProducts: Cannot open file", ioe)
        BitmapFactory.decodeResource(resources, placeholderId) ?: Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888).apply {
            Canvas().drawRect(0f,0f, width.toFloat(), height.toFloat(), Paint().apply {
                color = Color.GRAY
            })
        }
    }
}