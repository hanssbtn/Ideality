package com.example.ideality.managers

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(context: Context) {
    private val sharedPrefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxHistoryItems = 10

    fun addSearchQuery(query: String) {
        val history = getSearchHistory().toMutableList()
        if (query in history) history.remove(query)
        history.add(0, query)
        if (history.size > maxHistoryItems) history.removeAt(history.size - 1)

        sharedPrefs.edit().putString("history", gson.toJson(history)).apply()
    }

    fun getSearchHistory(): List<String> {
        val json = sharedPrefs.getString("history", null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun clearHistory() {
        sharedPrefs.edit().remove("history").apply()
    }
}