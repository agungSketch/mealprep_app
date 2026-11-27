package com.example.mealprep

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
        var latestGoal by remember { mutableStateOf<GoalType?>(null) }
        var latestMacroTarget by remember { mutableStateOf<MacroTarget?>(null) }
        var latestActivePlan by remember { mutableStateOf<RecommendedDayPlan?>(null) }

        val todayIso = "2026-11-17"
        var routineLogs by remember { mutableStateOf<Map<String, DailyRoutineState>>(emptyMap()) }

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
        fun applyPlanToRoutine(goal: GoalType, target: MacroTarget, plan: RecommendedDayPlan) {
            latestActivePlan = plan
            val mealChecks = plan.meals.map { DailyMealCheck(meal = it, isCompleted = false) }
            val existingToday = getRoutineForDate(todayIso)
            val updatedToday = existingToday.copy(
                dateIso = todayIso,
                activeGoal = goal,
                macroTarget = target,
                activePlan = plan,
                mealChecks = mealChecks
            )
            updateRoutineForDate(todayIso, updatedToday)
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
                        onUpdateRoutineState = { newState ->
                            updateRoutineForDate(dateIso, newState)
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
