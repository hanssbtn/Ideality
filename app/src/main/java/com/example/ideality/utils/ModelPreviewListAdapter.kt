package com.example.ideality.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.collection.LruCache
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.ideality.R

class ModelPreviewListAdapter(): Adapter<ModelPreviewListAdapter.ModelPreviewListViewHolder>() {
    private val tag = "ModelPreviewListAdapter"
    private val cache: LruCache<String, Bitmap>
    init {
        val cacheSize = Runtime.getRuntime().maxMemory() / 8
        Log.d(tag, "init: cache size set to ${cacheSize / 1024} KB")
        cache = object : LruCache<String, Bitmap>(cacheSize.toInt()) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                Log.d(tag, "cache.entryRemoved: Element is evicted: $evicted")
                Log.d(tag, "cache.entryRemoved: Removing entry with key $key")
                super.entryRemoved(evicted, key, oldValue, newValue)
            }

            override fun create(key: String): Bitmap? {
                Log.d(tag, "cache.create: Creating element with key $key")
                return super.create(key)
            }
        }
    }

    class ModelPreviewListViewHolder(view: View): ViewHolder(view) {
        val button: ImageButton = view.findViewById(R.id.model_list_element_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelPreviewListViewHolder {

        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.model_list_element, parent, false)
        return ModelPreviewListViewHolder(itemView)
    }

    override fun getItemCount() = 0

    override fun onBindViewHolder(holder: ModelPreviewListViewHolder, position: Int) {
//        val item = itemList[position]
        Log.d(tag, "onBindViewHolder")
//        holder.button.contentDescription = item.desc ?: ""
//        if (item.?imgUrl != null) {
//            holder.button.setImageBitmap(BitmapFactory.decodeFile(item.imgUrl))
//        }
    }


}