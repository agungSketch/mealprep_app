package com.example.mealprep

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.ui.AiFoodScannerScreen
import com.example.mealprep.ui.CalorieCalculatorScreen
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
}

private const val GEMINI_API_KEY = ""

@Composable
fun App() {

    MaterialTheme {
        var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.HeroSplash) }
        var latestGoal by remember { mutableStateOf<GoalType?>(null) }
        var latestMacroTarget by remember { mutableStateOf<MacroTarget?>(null) }

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
                        onOpenAiScanner = { currentRoute = AppRoute.AiScanner }
                    )
                }

                AppRoute.Calculator -> {
                    CalorieCalculatorScreen(
                        onBackToHome = { currentRoute = AppRoute.Home },
                        onCalculated = { goal, target ->
                            latestGoal = goal
                            latestMacroTarget = target
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
                        onBackToCalculator = { currentRoute = AppRoute.Calculator }
                    )
                }

                AppRoute.AiScanner -> {
                    AiFoodScannerScreen(
                        apiKey = GEMINI_API_KEY,
                        activeMacroTarget = latestMacroTarget,
                        onBackToHome = { currentRoute = AppRoute.Home }
                    )
                }
            }
        }
    }
}