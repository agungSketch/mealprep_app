package com.example.mealprep.data

import android.content.Context
import android.content.SharedPreferences

actual class KeyValueStorage {
    private val prefs: SharedPreferences? by lazy {
        ApplicationContextProvider.context?.getSharedPreferences("meal_prep_storage", Context.MODE_PRIVATE)
    }

    actual fun getString(key: String): String? {
        return prefs?.getString(key, null)
    }

    actual fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }
}

object ApplicationContextProvider {
    var context: Context? = null
}
