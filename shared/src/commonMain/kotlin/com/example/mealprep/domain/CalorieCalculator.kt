package com.example.mealprep.domain

import kotlin.math.roundToInt

enum class Gender { MALE, FEMALE }

enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),      // Sangat jarang / kerja kantoran
    LIGHT(1.375),       // Ringan (1-3 hari/minggu)
    MODERATE(1.55),      // Sedang (3-5 hari/minggu)
    ACTIVE(1.725)        // Berat (6-7 hari/minggu)
}

enum class GoalType(val label: String, val calorieDelta: Int) {
    CUT("Defisit (Turun BB)", -400),
    MAINTAIN("Maintain (Jaga BB)", 0),
    BULK("Surplus (Naik BB / Massa Otot)", 350)
}

data class MacroTarget(
    val tdeeCalories: Int,
    val targetCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
)

object CalorieCalculator {
    fun calculateTdee(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        gender: Gender,
        activityLevel: ActivityLevel
    ): Double {
        val bmr = if (gender == Gender.MALE) {
            (10 * weightKg) + (6.25 * heightCm) - (5 * ageYears) + 5
        } else {
            (10 * weightKg) + (6.25 * heightCm) - (5 * ageYears) - 161
        }
        return bmr * activityLevel.multiplier
    }

    fun calculateMacros(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        gender: Gender,
        activityLevel: ActivityLevel,
        goal: GoalType
    ): MacroTarget {
        val tdee = calculateTdee(weightKg, heightCm, ageYears, gender, activityLevel).roundToInt()
        val targetCalories = (tdee + goal.calorieDelta).coerceAtLeast(1200)

        // Estimasi praktis: protein ~1.8g/kg BB, lemak ~25% kalori, sisa kalori ke karbohidrat
        val proteinGrams = (weightKg * 1.8).roundToInt()
        val fatGrams = ((targetCalories * 0.25) / 9.0).roundToInt()

        val usedCalories = (proteinGrams * 4) + (fatGrams * 9)
        val carbsGrams = ((targetCalories - usedCalories).coerceAtLeast(0) / 4.0).roundToInt()

        return MacroTarget(
            tdeeCalories = tdee,
            targetCalories = targetCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams
        )
    }
}