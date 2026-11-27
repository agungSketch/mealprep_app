package com.example.mealprep

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mealprep.data.AppPersistedData
import com.example.mealprep.data.LocalMealRepository
import com.example.mealprep.data.SavedMealPlanTemplate
import com.example.mealprep.domain.DailyMealCheck
import com.example.mealprep.domain.DailyRoutineState
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.domain.MealRecommendationEngine
import com.example.mealprep.domain.RecommendedDayPlan
import com.example.mealprep.share.AchievementShareData
import com.example.mealprep.ui.AchievementShareDialog
import com.example.mealprep.ui.AiFoodScannerScreen
import com.example.mealprep.ui.CalorieCalculatorScreen
import com.example.mealprep.ui.DailyRoutineScreen
import com.example.mealprep.ui.HomeScreen
import com.example.mealprep.ui.MealPlanResultScreen
import com.example.mealprep.ui.RoutineCalendarScreen
import com.example.mealprep.ui.SplashScreen

private enum class MainTab(val label: String, val icon: String) {
    HOME("Home", "🏠"),
    CALCULATOR("Kalori", "🥗"),
    SCANNER("AI Scan", "📸"),
    JOURNAL("Jurnal", "📓")
}

private sealed interface AppRoute {
    data object HeroSplash : AppRoute
    data object MainShell : AppRoute
    data class MealResult(
        val goal: GoalType,
        val macroTarget: MacroTarget
    ) : AppRoute
    data class DailyRoutine(val dateIso: String) : AppRoute
}

private const val GEMINI_API_KEY = BuildKonfig.GEMINI_API_KEY

@Composable
fun App() {

    MaterialTheme {
        var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.HeroSplash) }
        var currentTab by remember { mutableStateOf(MainTab.HOME) }

        // Local Storage Repository
        val repository = remember { LocalMealRepository() }
        val initialPersisted = remember { repository.loadPersistedData() }

        var latestGoal by remember { mutableStateOf(initialPersisted.latestGoal) }
        var latestMacroTarget by remember { mutableStateOf(initialPersisted.latestMacroTarget) }
        var latestActivePlan by remember { mutableStateOf(initialPersisted.latestActivePlan) }
        var routineLogs by remember { mutableStateOf(initialPersisted.routineLogs) }
        var savedTemplates by remember { mutableStateOf(initialPersisted.savedTemplates) }
        var showShareDialog by remember { mutableStateOf(false) }

        // Otomatis simpan data ke penyimpanan lokal perangkat setiap ada perubahan
        LaunchedEffect(routineLogs, latestGoal, latestMacroTarget, latestActivePlan, savedTemplates) {
            repository.savePersistedData(
                AppPersistedData(
                    routineLogs = routineLogs,
                    latestGoal = latestGoal,
                    latestMacroTarget = latestMacroTarget,
                    latestActivePlan = latestActivePlan,
                    savedTemplates = savedTemplates
                )
            )
        }

        val todayIso = getTodayDateIso()

        fun getRoutineForDate(dateIso: String): DailyRoutineState {
            return routineLogs[dateIso] ?: DailyRoutineState(
                dateIso = dateIso,
                activeGoal = latestGoal,
                macroTarget = latestMacroTarget,
                activePlan = latestActivePlan,
                mealChecks = latestActivePlan?.meals?.map { DailyMealCheck(meal = it, isCompleted = false) } ?: emptyList()
            )
        }

        fun updateRoutineForDate(dateIso: String, newState: DailyRoutineState) {
            routineLogs = routineLogs + (dateIso to newState)
        }

        // Helper untuk sinkronisasi rencana menu ke Daily Routine
        fun applyPlanToRoutine(goal: GoalType, target: MacroTarget, plan: RecommendedDayPlan, dateIso: String = todayIso) {
            latestActivePlan = plan
            val mealChecks = plan.meals.map { DailyMealCheck(meal = it, isCompleted = false) }
            val existingState = getRoutineForDate(dateIso)
            val updatedState = existingState.copy(
                dateIso = dateIso,
                activeGoal = goal,
                macroTarget = target,
                activePlan = plan,
                mealChecks = mealChecks
            )
            updateRoutineForDate(dateIso, updatedState)

            // Auto simpan ke pustaka template lokal agar bisa dipakai lagi tanpa token AI
            val mealsKey = plan.meals.joinToString("-") { it.id }
            val firstMealName = plan.meals.firstOrNull()?.name?.take(22) ?: "Sehat"
            val templateTitle = "Menu $firstMealName (${plan.totalPlannedCalories} kcal)"
            val newTemplate = SavedMealPlanTemplate(
                id = "tpl_${mealsKey}_${plan.totalPlannedCalories}",
                title = templateTitle,
                dateCreatedIso = dateIso,
                plan = plan
            )
            if (savedTemplates.none { it.id == newTemplate.id }) {
                savedTemplates = savedTemplates + newTemplate
            }
        }

        val currentTodayState = getRoutineForDate(todayIso)
        val achievedDaysCount = routineLogs.values.count { it.isDayGoalAchieved }
        val activeDaysCount = routineLogs.values.count { it.consumedCalories > 0 || it.waterIntakeMl > 0 }

        if (showShareDialog) {
            val highlightMealName = currentTodayState.mealChecks.firstOrNull { it.isCompleted }?.meal?.name
                ?: latestActivePlan?.meals?.firstOrNull()?.name

            AchievementShareDialog(
                data = AchievementShareData(
                    goal = latestGoal,
                    macroTarget = latestMacroTarget,
                    todayCalories = currentTodayState.consumedCalories,
                    todayProteinGrams = currentTodayState.consumedProtein,
                    todayWaterMl = currentTodayState.waterIntakeMl,
                    onTrackDays = achievedDaysCount.coerceAtLeast(1),
                    highlightMeal = highlightMealName
                ),
                onDismiss = { showShareDialog = false }
            )
        }

        Crossfade(
            targetState = currentRoute,
            animationSpec = tween(durationMillis = 250)
        ) { route ->
            when (route) {
                AppRoute.HeroSplash -> {
                    SplashScreen(
                        onFinished = { currentRoute = AppRoute.MainShell }
                    )
                }

                AppRoute.MainShell -> {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                MainTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Text(text = tab.icon) },
                                        label = { Text(text = tab.label) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentTab) {
                                MainTab.HOME -> {
                                    HomeScreen(
                                        activeGoal = latestGoal,
                                        activeMacroTarget = latestMacroTarget,
                                        todayCalories = currentTodayState.consumedCalories,
                                        todayProteinGrams = currentTodayState.consumedProtein,
                                        todayWaterMl = currentTodayState.waterIntakeMl,
                                        weeklyActiveDays = activeDaysCount.coerceAtLeast(1),
                                        weeklyOnTrackDays = achievedDaysCount.coerceAtLeast(1),
                                        onOpenCalculator = { currentTab = MainTab.CALCULATOR },
                                        onOpenMealResult = {
                                            val goal = latestGoal
                                            val target = latestMacroTarget
                                            if (goal != null && target != null) {
                                                currentRoute = AppRoute.MealResult(goal, target)
                                            } else {
                                                currentTab = MainTab.CALCULATOR
                                            }
                                        },
                                        onOpenJournalCalendar = { currentTab = MainTab.JOURNAL },
                                        onOpenTodayJournal = { currentRoute = AppRoute.DailyRoutine(dateIso = todayIso) },
                                        onOpenAiScanner = { currentTab = MainTab.SCANNER },
                                        onOpenIngredientRecipe = { currentTab = MainTab.SCANNER }
                                    )
                                }

                                MainTab.CALCULATOR -> {
                                    CalorieCalculatorScreen(
                                        onBackToHome = { currentTab = MainTab.HOME },
                                        onCalculated = { goal, target ->
                                            latestGoal = goal
                                            latestMacroTarget = target
                                            val defaultPlan = MealRecommendationEngine.generateDayPlan(target)
                                            applyPlanToRoutine(goal, target, defaultPlan)
                                            currentRoute = AppRoute.MealResult(goal, target)
                                        }
                                    )
                                }

                                MainTab.SCANNER -> {
                                    AiFoodScannerScreen(
                                        apiKey = GEMINI_API_KEY,
                                        activeGoal = latestGoal,
                                        activeMacroTarget = latestMacroTarget,
                                        onApplyPlanAndOpenRoutine = { plan ->
                                            val goal = latestGoal ?: GoalType.MAINTAIN
                                            val target = latestMacroTarget ?: MacroTarget(
                                                tdeeCalories = plan.targetCalories,
                                                targetCalories = plan.targetCalories,
                                                proteinGrams = plan.totalProteinGrams,
                                                carbsGrams = plan.totalCarbsGrams,
                                                fatGrams = plan.totalFatGrams
                                            )
                                            applyPlanToRoutine(goal, target, plan)
                                            currentTab = MainTab.JOURNAL
                                        },
                                        onBackToHome = { currentTab = MainTab.HOME }
                                    )
                                }

                                MainTab.JOURNAL -> {
                                    RoutineCalendarScreen(
                                        routineLogs = routineLogs,
                                        todayIso = todayIso,
                                        onSelectDate = { selectedDate ->
                                            currentRoute = AppRoute.DailyRoutine(dateIso = selectedDate)
                                        },
                                        onBackToHome = { currentTab = MainTab.HOME }
                                    )
                                }
                            }
                        }
                    }
                }

                is AppRoute.MealResult -> {
                    MealPlanResultScreen(
                        goal = route.goal,
                        macroTarget = route.macroTarget,
                        apiKey = GEMINI_API_KEY,
                        onBackToCalculator = {
                            currentTab = MainTab.CALCULATOR
                            currentRoute = AppRoute.MainShell
                        },
                        onOpenDailyRoutine = { plan ->
                            applyPlanToRoutine(route.goal, route.macroTarget, plan)
                            currentTab = MainTab.JOURNAL
                            currentRoute = AppRoute.MainShell
                        }
                    )
                }

                is AppRoute.DailyRoutine -> {
                    val dateIso = route.dateIso
                    val selectedState = getRoutineForDate(dateIso)

                    DailyRoutineScreen(
                        routineState = selectedState,
                        savedTemplates = savedTemplates,
                        apiKey = GEMINI_API_KEY,
                        onUpdateRoutineState = { newState ->
                            updateRoutineForDate(dateIso, newState)
                        },
                        onApplyPlanToDate = { targetDateIso, plan ->
                            val goal = latestGoal ?: GoalType.MAINTAIN
                            val target = latestMacroTarget ?: MacroTarget(
                                tdeeCalories = plan.targetCalories,
                                targetCalories = plan.targetCalories,
                                proteinGrams = plan.totalProteinGrams,
                                carbsGrams = plan.totalCarbsGrams,
                                fatGrams = plan.totalFatGrams
                            )
                            applyPlanToRoutine(goal, target, plan, dateIso = targetDateIso)
                        },
                        onOpenCalculator = {
                            currentTab = MainTab.CALCULATOR
                            currentRoute = AppRoute.MainShell
                        },
                        onOpenMealResult = {
                            val goal = latestGoal
                            val target = latestMacroTarget
                            if (goal != null && target != null) {
                                currentRoute = AppRoute.MealResult(goal = goal, macroTarget = target)
                            } else {
                                currentTab = MainTab.CALCULATOR
                                currentRoute = AppRoute.MainShell
                            }
                        },
                        onBackToCalendar = {
                            currentTab = MainTab.JOURNAL
                            currentRoute = AppRoute.MainShell
                        }
                    )
                }
            }
        }
    }
}
