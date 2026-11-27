package com.example.mealprep.share

import android.content.Context
import android.content.Intent

object AndroidShareContext {
    var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}

actual fun shareAchievementText(text: String) {
    val ctx = AndroidShareContext.context ?: return
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val shareIntent = Intent.createChooser(sendIntent, "Bagikan Pencapaian GramPulse AI").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(shareIntent)
}
