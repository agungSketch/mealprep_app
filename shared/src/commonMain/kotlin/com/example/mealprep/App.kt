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
    data object DailyRoutine : AppRoute
}

private const val GEMINI_API_KEY = ""

@Composable
fun App() {

    MaterialTheme {
        var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.HeroSplash) }
        var latestGoal by remember { mutableStateOf<GoalType?>(null) }
        var latestMacroTarget by remember { mutableStateOf<MacroTarget?>(null) }
        var routineState by remember { mutableStateOf(DailyRoutineState()) }

        // Helper untuk sinkronisasi rencana menu ke Daily Routine
        fun applyPlanToRoutine(goal: GoalType, target: MacroTarget, plan: RecommendedDayPlan) {
            val mealChecks = plan.meals.map { DailyMealCheck(meal = it, isCompleted = false) }
            routineState = routineState.copy(
                activeGoal = goal,
                macroTarget = target,
                activePlan = plan,
                mealChecks = mealChecks
            )
        }

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
                        routineState = routineState,
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
                        onOpenDailyRoutine = { currentRoute = AppRoute.DailyRoutine }
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
                            currentRoute = AppRoute.DailyRoutine
                        }
                    )
                }

                AppRoute.AiScanner -> {
                    AiFoodScannerScreen(
                        apiKey = GEMINI_API_KEY,
                        activeMacroTarget = latestMacroTarget,
                        onBackToHome = { currentRoute = AppRoute.Home }
                    )
                }

                AppRoute.DailyRoutine -> {
                    DailyRoutineScreen(
                        routineState = routineState,
                        onUpdateRoutineState = { newState -> routineState = newState },
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
                        onBackToHome = { currentRoute = AppRoute.Home }
                    )
                }
            }
        }
    }
}
