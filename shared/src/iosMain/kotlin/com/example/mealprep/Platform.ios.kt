package com.example.mealprep

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun rememberAppSettingsLauncher(): () -> Unit {
    return {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    }
}