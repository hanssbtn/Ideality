package com.example.ideality.viewmodels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ideality.utils.ProductDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewListElementViewModel(private val repo: ProductDataRepository): ViewModel() {
    val tag = "PreviewListElementViewModel"

    val images: MutableLiveData<ArrayList<Pair<String, Bitmap>>> by lazy {
        MutableLiveData<ArrayList<Pair<String, Bitmap>>>(ArrayList())
    }

    init {
        Log.d(tag, "init: Initialized view model.")
        viewModelScope.launch {
            fetchData()
        }
    }

    private suspend fun fetchData() {
        return withContext(Dispatchers.IO) {
            Log.d(tag, "fetch: Fetching data from repository...")
            val initialSize = 15
            try {
                Log.d(tag, "fetch: Fetching $initialSize files to cache")
                val res = repo.fetchBitmapsFromDirectory(count = initialSize)
                res.forEach {
                    images.value!!.add(it)
                }
            } catch (re: RuntimeException) {
                Log.d(tag, re.stackTraceToString())
            }
        }
    }
}