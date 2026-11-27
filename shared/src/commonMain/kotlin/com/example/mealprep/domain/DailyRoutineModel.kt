package com.example.mealprep.domain

data class DailyMealCheck(
    val meal: ScaledMeal,
    val isCompleted: Boolean = false
)

data class DailyHabit(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isCompleted: Boolean = false
)

data class DailyRoutineState(
    val activeGoal: GoalType? = null,
    val macroTarget: MacroTarget? = null,
    val activePlan: RecommendedDayPlan? = null,
    val mealChecks: List<DailyMealCheck> = emptyList(),
    val waterIntakeMl: Int = 0,
    val waterTargetMl: Int = 2500,
    val habits: List<DailyHabit> = defaultHabits
) {
    val consumedCalories: Int
        get() = mealChecks.filter { it.isCompleted }.sumOf { it.meal.calories }

    val consumedProtein: Int
        get() = mealChecks.filter { it.isCompleted }.sumOf { it.meal.proteinGrams }

    val consumedCarbs: Int
        get() = mealChecks.filter { it.isCompleted }.sumOf { it.meal.carbsGrams }

    val consumedFat: Int
        get() = mealChecks.filter { it.isCompleted }.sumOf { it.meal.fatGrams }

    val completedMealsCount: Int
        get() = mealChecks.count { it.isCompleted }

    val completedHabitsCount: Int
        get() = habits.count { it.isCompleted }

    companion object {
        val defaultHabits = listOf(
            DailyHabit("h1", "Minum Air 2.5 Litur", "Jaga hidrasi tubuh sepanjang hari", "💧"),
            DailyHabit("h2", "Olahraga 30 Menit", "Jalan cepat, gym, atau cardio ringan", "🏃‍♂️"),
            DailyHabit("h3", "Tidur Cukup 7-8 Jam", "Istirahat cukup untuk pemulihan otot", "😴"),
            DailyHabit("h4", "Batasi Gula Tambahan", "Hindari minuman manis berlebih", "🍎")
        )
    }
}
