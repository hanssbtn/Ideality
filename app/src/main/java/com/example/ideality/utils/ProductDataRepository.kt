package com.example.ideality.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.LruCache
import androidx.core.graphics.scale
import com.example.ideality.R
import java.io.File
import java.security.InvalidParameterException
import kotlin.jvm.Throws

class ProductDataRepository(private val context: Context) {
    val tag = "ProductDataRepository"

    @Throws(InvalidParameterException::class, NoSuchFileException::class)
    fun fetchBitmapFromFile(filename: String, dir: String = context.filesDir.path): Bitmap {
        val path = "$dir/$filename"
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var bmp: Bitmap? = BitmapFactory.decodeFile(path, options)
        if (bmp.height == -1 || bmp.width == -1) {
            throw InvalidParameterException("Invalid filename parameter: $path")
        }
        options.inJustDecodeBounds = false
        bmp = BitmapFactory.decodeFile(path, options)
        if (bmp == null) {
            throw RuntimeException("Cannot convert file $path to bitmap.")
        }
        Log.d(tag, "fetchBitmapFromFile: Size: ${bmp.width}x${bmp.height}px")
        Log.d(tag, "fetchBitmapFromFile: Density: ${bmp.density} %")
        Log.d(tag, "fetchBitmapFromFile: Allocated bytes: ${bmp.allocationByteCount} bytes")
        return bmp
    }

    fun fetchBitmapsFromDirectory(path: String = context.filesDir.path, count: Int = Int.MAX_VALUE): ArrayList<Pair<String, Bitmap>> {
        if (count <= 0) throw InvalidParameterException("count must be > 0 (got $count)")
        val dir = File(path)
        val options = BitmapFactory.Options()
        if (!(dir.exists() && dir.isDirectory)) {
            throw RuntimeException("Directory cannot be opened/is not a directory.")
        }
        val fileTree = dir.walkTopDown()
            .onEnter { file: File ->
                Log.d(tag,"fetchBitmapsFromDirectory: Entering file ${file.name}")

                true
            }
            .onLeave { file ->
                Log.d(tag,"fetchBitmapsFromDirectory: Leaving file ${file.name}")

            }
            .onFail { _, ioException ->
                Log.d(
                    tag,
                    "fetchBitmapsFromDirectory: failed to open $path. Reason: $ioException"
                )
            }
        return if (count >= fileTree.count()) {
            ArrayList(fileTree.map {
                Log.d(tag,"fetchBitmapsFromDirectory.fileTree.map: Decoding and rescaling ${it.name}")
                Pair(it.name, BitmapFactory.decodeFile(it.absolutePath, options).scale(R.dimen.model_preview_list_element_size, R.dimen.model_preview_list_element_size))
            }.toList())
        } else {
            ArrayList(fileTree.onEachIndexed { index, file ->
                if (count == index) return@onEachIndexed
            }.map {
                Log.d(tag,"fetchBitmapsFromDirectory.fileTree.map: Decoding and rescaling ${it.name}")
                Pair(it.name, BitmapFactory.decodeFile(it.absolutePath, options).scale(R.dimen.model_preview_list_element_size, R.dimen.model_preview_list_element_size))
            }.toList())
        }
    }
}