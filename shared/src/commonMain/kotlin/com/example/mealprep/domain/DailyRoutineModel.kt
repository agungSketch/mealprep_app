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
    val dateIso: String = "2026-11-17",
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

    val isDayGoalAchieved: Boolean
        get() = (completedMealsCount >= 3 || (macroTarget != null && consumedCalories >= macroTarget.targetCalories * 0.9))

    companion object {
        val defaultHabits = listOf(
            DailyHabit("h1", "Minum Air 2.5 Liter", "Jaga hidrasi tubuh sepanjang hari", "💧"),
            DailyHabit("h2", "Olahraga 30 Menit", "Jalan cepat, gym, atau cardio ringan", "🏃‍♂️"),
            DailyHabit("h3", "Tidur Cukup 7-8 Jam", "Istirahat cukup untuk pemulihan otot", "😴"),
            DailyHabit("h4", "Batasi Gula Tambahan", "Hindari minuman manis berlebih", "🍎")
        )
    }
}

data class CalendarDayInfo(
    val dayNumber: Int,
    val dateIso: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

object DateUtils {
    fun formatDateIso(year: Int, month: Int, day: Int): String {
        val m = if (month < 10) "0$month" else "$month"
        val d = if (day < 10) "0$day" else "$day"
        return "$year-$m-$d"
    }

    fun parseDateIso(dateIso: String): Triple<Int, Int, Int> {
        val parts = dateIso.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 11
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 17
        return Triple(year, month, day)
    }

    fun getMonthName(monthNumber: Int): String {
        return when (monthNumber) {
            1 -> "Januari"
            2 -> "Februari"
            3 -> "Maret"
            4 -> "April"
            5 -> "Mei"
            6 -> "Juni"
            7 -> "Juli"
            8 -> "Agustus"
            9 -> "September"
            10 -> "Oktober"
            11 -> "November"
            12 -> "Desember"
            else -> "November"
        }
    }

    fun getDayOfWeekName(year: Int, month: Int, day: Int): String {
        val firstDay = getFirstDayOfWeek(year, month)
        val dayOfWeekIndex = (firstDay + (day - 1)) % 7
        return when (dayOfWeekIndex) {
            0 -> "Senin"
            1 -> "Selasa"
            2 -> "Rabu"
            3 -> "Kamis"
            4 -> "Jumat"
            5 -> "Sabtu"
            6 -> "Minggu"
            else -> "Senin"
        }
    }

    fun formatIndonesianDate(dateIso: String): String {
        val (year, month, day) = parseDateIso(dateIso)
        val dayName = getDayOfWeekName(year, month, day)
        val monthName = getMonthName(month)
        return "$dayName, $day $monthName $year"
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    // 0 = Monday, ..., 6 = Sunday
    fun getFirstDayOfWeek(year: Int, month: Int): Int {
        val m = if (month < 3) month + 12 else month
        val y = if (month < 3) year - 1 else year
        val k = y % 100
        val j = y / 100
        val h = (1 + (13 * (m + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
        return (h + 5) % 7
    }

    fun generateMonthGrid(year: Int, month: Int, todayIso: String = "2026-11-17"): List<CalendarDayInfo> {
        val firstDayOfWeek = getFirstDayOfWeek(year, month)
        val daysInMonth = getDaysInMonth(year, month)

        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year
        val prevMonthDays = getDaysInMonth(prevYear, prevMonth)

        val grid = mutableListOf<CalendarDayInfo>()

        // Previous month trailing days
        for (i in (firstDayOfWeek - 1) downTo 0) {
            val day = prevMonthDays - i
            val dateIso = formatDateIso(prevYear, prevMonth, day)
            grid.add(CalendarDayInfo(dayNumber = day, dateIso = dateIso, isCurrentMonth = false, isToday = (dateIso == todayIso)))
        }

        // Current month days
        for (day in 1..daysInMonth) {
            val dateIso = formatDateIso(year, month, day)
            grid.add(CalendarDayInfo(dayNumber = day, dateIso = dateIso, isCurrentMonth = true, isToday = (dateIso == todayIso)))
        }

        // Next month leading days
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        var nextDay = 1
        while (grid.size % 7 != 0 || grid.size < 35) {
            val dateIso = formatDateIso(nextYear, nextMonth, nextDay)
            grid.add(CalendarDayInfo(dayNumber = nextDay, dateIso = dateIso, isCurrentMonth = false, isToday = (dateIso == todayIso)))
            nextDay++
        }

        return grid
    }
}
