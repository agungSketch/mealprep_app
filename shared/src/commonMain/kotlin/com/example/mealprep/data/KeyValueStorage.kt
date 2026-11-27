package com.example.mealprep.data

expect class KeyValueStorage() {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}
