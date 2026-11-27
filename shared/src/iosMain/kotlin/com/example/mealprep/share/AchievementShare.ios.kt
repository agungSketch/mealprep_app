package com.example.mealprep.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareAchievementText(text: String) {
    val currentViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val activityItems = listOf(text)
    val activityViewController = UIActivityViewController(
        activityItems = activityItems,
        applicationActivities = null
    )
    currentViewController.presentViewController(
        activityViewController,
        animated = true,
        completion = null
    )
}
