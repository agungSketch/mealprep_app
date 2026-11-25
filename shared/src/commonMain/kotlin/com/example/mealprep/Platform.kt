package com.example.mealprep

import androidx.compose.runtime.Composable

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun rememberAppSettingsLauncher(): () -> Unit