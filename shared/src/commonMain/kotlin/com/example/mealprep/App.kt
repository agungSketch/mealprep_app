package com.example.mealprep

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.example.mealprep.data.AppPersistedData
import com.example.mealprep.data.LocalMealRepository
import com.example.mealprep.data.SavedMealPlanTemplate
import com.example.mealprep.domain.DailyMealCheck
import com.example.mealprep.domain.DailyRoutineState
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.domain.MealRecommendationEngine
import com.example.mealprep.domain.RecommendedDayPlan
import com.example.mealprep.ui.AiFoodScannerScreen
import com.example.mealprep.ui.CalorieCalculatorScreen
import com.example.mealprep.ui.DailyRoutineScreen
import com.example.mealprep.ui.HomeScreen
import com.example.mealprep.ui.MealPlanResultScreen
import com.example.mealprep.ui.RoutineCalendarScreen
import com.example.mealprep.ui.SplashScreen

private sealed interface AppRoute {
    data object HeroSplash : AppRoute
    data object Home : AppRoute
    data object Calculator : AppRoute
    data class MealResult(
        val goal: GoalType,
        val macroTarget: MacroTarget
    ) : AppRoute
    data object AiScanner : AppRoute
    data object RoutineCalendar : AppRoute
    data class DailyRoutine(val dateIso: String) : AppRoute
}

private const val GEMINI_API_KEY = BuildKonfig.GEMINI_API_KEY

@Composable
fun App() {

    MaterialTheme {
        var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.HeroSplash) }

        // Local Storage Repository
        val repository = remember { LocalMealRepository() }
        val initialPersisted = remember { repository.loadPersistedData() }

        var latestGoal by remember { mutableStateOf(initialPersisted.latestGoal) }
        var latestMacroTarget by remember { mutableStateOf(initialPersisted.latestMacroTarget) }
        var latestActivePlan by remember { mutableStateOf(initialPersisted.latestActivePlan) }
        var routineLogs by remember { mutableStateOf(initialPersisted.routineLogs) }
        var savedTemplates by remember { mutableStateOf(initialPersisted.savedTemplates) }

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
            val templateTitle = "Menu ${plan.meals.firstOrNull()?.name?.take(20) ?: "Sehat"} (${plan.totalPlannedCalories} kcal)"
            val newTemplate = SavedMealPlanTemplate(
                id = "tpl_${dateIso}_${plan.totalPlannedCalories}",
                title = templateTitle,
                dateCreatedIso = dateIso,
                plan = plan
            )
            if (savedTemplates.none { it.id == newTemplate.id }) {
                savedTemplates = savedTemplates + newTemplate
            }
        }

        val currentTodayState = getRoutineForDate(todayIso)

        Crossfade(
            targetState = currentRoute,
            animationSpec = tween(durationMillis = 300)
        ) { route ->
            when (route) {
                AppRoute.HeroSplash -> {
                    SplashScreen(
                        onFinished = { currentRoute = AppRoute.Home }
                    )
                }

                AppRoute.Home -> {
                    HomeScreen(
                        activeGoal = latestGoal,
                        activeMacroTarget = latestMacroTarget,
                        routineState = currentTodayState,
                        onOpenCalculator = { currentRoute = AppRoute.Calculator },
                        onOpenMealResult = {
                            val goal = latestGoal
                            val target = latestMacroTarget
                            if (goal != null && target != null) {
                                currentRoute = AppRoute.MealResult(goal = goal, macroTarget = target)
                            } else {
                                currentRoute = AppRoute.Calculator
                            }
                        },
                        onOpenAiScanner = { currentRoute = AppRoute.AiScanner },
                        onOpenRoutineCalendar = { currentRoute = AppRoute.RoutineCalendar }
                    )
                }

                AppRoute.Calculator -> {
                    CalorieCalculatorScreen(
                        onBackToHome = { currentRoute = AppRoute.Home },
                        onCalculated = { goal, target ->
                            latestGoal = goal
                            latestMacroTarget = target
                            val defaultPlan = MealRecommendationEngine.generateDayPlan(target)
                            applyPlanToRoutine(goal, target, defaultPlan)
                            currentRoute = AppRoute.MealResult(
                                goal = goal,
                                macroTarget = target
                            )
                        }
                    )
                }

                is AppRoute.MealResult -> {
                    MealPlanResultScreen(
                        goal = route.goal,
                        macroTarget = route.macroTarget,
                        apiKey = GEMINI_API_KEY,
                        onBackToCalculator = { currentRoute = AppRoute.Calculator },
                        onOpenDailyRoutine = { plan ->
                            applyPlanToRoutine(route.goal, route.macroTarget, plan)
                            currentRoute = AppRoute.RoutineCalendar
                        }
                    )
                }

                AppRoute.AiScanner -> {
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
                            currentRoute = AppRoute.RoutineCalendar
                        },
                        onBackToHome = { currentRoute = AppRoute.Home }
                    )
                }

                AppRoute.RoutineCalendar -> {
                    RoutineCalendarScreen(
                        routineLogs = routineLogs,
                        todayIso = todayIso,
                        onSelectDate = { selectedDate ->
                            currentRoute = AppRoute.DailyRoutine(dateIso = selectedDate)
                        },
                        onBackToHome = { currentRoute = AppRoute.Home }
                    )
                }

                is AppRoute.DailyRoutine -> {
                    val dateIso = route.dateIso
                    val selectedState = getRoutineForDate(dateIso)

                    DailyRoutineScreen(
                        routineState = selectedState,
                        savedTemplates = savedTemplates,
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
                        onOpenCalculator = { currentRoute = AppRoute.Calculator },
                        onOpenMealResult = {
                            val goal = latestGoal
                            val target = latestMacroTarget
                            if (goal != null && target != null) {
                                currentRoute = AppRoute.MealResult(goal = goal, macroTarget = target)
                            } else {
                                currentRoute = AppRoute.Calculator
                            }
                        },
                        onBackToCalendar = { currentRoute = AppRoute.RoutineCalendar }
                    )
                }
            }
        }
    }
}
