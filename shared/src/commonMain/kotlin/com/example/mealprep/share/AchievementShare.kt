package com.example.mealprep.share

import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget

data class AchievementShareData(
    val goal: GoalType?,
    val macroTarget: MacroTarget?,
    val todayCalories: Int,
    val todayProteinGrams: Int,
    val todayWaterMl: Int,
    val onTrackDays: Int,
    val highlightMeal: String? = null
) {
    fun buildShareText(): String {
        val targetCal = macroTarget?.targetCalories ?: 0
        val targetPro = macroTarget?.proteinGrams ?: 0
        val goalText = goal?.label ?: "Program Nutrisi & Latihan"
        val mealLine = highlightMeal?.let { "\n🍽️ Menu/Log Terbaru: $it" } ?: ""

        return buildString {
            appendLine("🏆 GRAMPULSE AI — ACHIEVEMENT LOG")
            appendLine("🎯 Target: $goalText")
            if (targetCal > 0) {
                appendLine("🔥 Kalori: $todayCalories / $targetCal kcal")
                appendLine("💪 Protein: ${todayProteinGrams}g / ${targetPro}g")
            } else {
                appendLine("🔥 Kalori Tercatat: $todayCalories kcal")
                appendLine("💪 Protein Tercatat: ${todayProteinGrams}g")
            }
            appendLine("💧 Hidrasi: $todayWaterMl ml")
            appendLine("🔥 Konsistensi: $onTrackDays Hari On-Track")
            append(mealLine)
            append("\n#GramPulseAI #MacroTracking #FitnessJourney")
        }.trim()
    }
}

expect fun shareAchievementText(text: String)
