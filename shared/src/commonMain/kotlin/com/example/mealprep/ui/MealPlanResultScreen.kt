package com.example.mealprep.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealprep.data.GeminiMealPlannerService
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.domain.MealRecommendationEngine
import com.example.mealprep.domain.RecommendedDayPlan
import com.example.mealprep.domain.ScaledMeal
import kotlinx.coroutines.launch

@Composable
fun MealPlanResultScreen(
    goal: GoalType,
    macroTarget: MacroTarget,
    apiKey: String = "",
    onBackToCalculator: () -> Unit,
    onOpenDailyRoutine: (RecommendedDayPlan) -> Unit
) {
    val scope = rememberCoroutineScope()
    var userApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    val plannerService = remember(userApiKey) { GeminiMealPlannerService(apiKey = userApiKey) }

    var aiDayPlan by remember(macroTarget, goal) { mutableStateOf<RecommendedDayPlan?>(null) }
    var isGeneratingAi by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Instant initial plan so user sees UI with zero latency
    val initialPlan = remember(macroTarget) {
        MealRecommendationEngine.generateDayPlan(macroTarget = macroTarget)
    }

    val activePlan = aiDayPlan ?: initialPlan

    fun loadAiMealPlan() {
        isGeneratingAi = true
        errorMessage = null
        scope.launch {
            val result = plannerService.generateAiMealPlan(goal, macroTarget)
            isGeneratingAi = false
            result
                .onSuccess { aiDayPlan = it }
                .onFailure { errorMessage = it.message ?: "Gagal meracik resep dengan Gemini AI." }
        }
    }

    // Background load Gemini AI plan
    LaunchedEffect(goal, macroTarget) {
        loadAiMealPlan()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Langkah 2 dari 2 • Hasil Nutrisi & Menu",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(
                onClick = onBackToCalculator,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Ubah Data", fontSize = 12.sp)
            }
        }

        // Target Kalori & Makro
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Target Harian (${goal.label})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${macroTarget.targetCalories} kcal / hari",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "TDEE Dasar: ${macroTarget.tdeeCalories} kcal / hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroItem(label = "Protein", value = "${macroTarget.proteinGrams}g")
                    MacroItem(label = "Karbo", value = "${macroTarget.carbsGrams}g")
                    MacroItem(label = "Lemak", value = "${macroTarget.fatGrams}g")
                }
            }
        }

        // Tombol Gunakan ke Daily Routine
        Button(
            onClick = { onOpenDailyRoutine(activePlan) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("📅 Mulai Jurnal Daily Routine", fontWeight = FontWeight.Bold)
        }

        // Banner Status Loading AI (Background)
        AnimatedVisibility(visible = isGeneratingAi) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Gemini AI Flash Lite sedang meracik variasi menu kustom...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error message if any
        errorMessage?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { loadAiMealPlan() }) {
                        Text("Coba Lagi")
                    }
                }
            }
        }

        // Judul List & Tombol Acak Menu AI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (aiDayPlan != null) "✨ Resep Kustom Gemini AI" else "🍽️ Saran Menu & Resep Harian",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${activePlan.totalPlannedCalories} kcal (P ${activePlan.totalProteinGrams}g • K ${activePlan.totalCarbsGrams}g • L ${activePlan.totalFatGrams}g)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { loadAiMealPlan() },
                enabled = !isGeneratingAi,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("✨ Acak Menu AI", fontSize = 12.sp)
            }
        }

        // Daftar Resep
        activePlan.meals.forEach { meal ->
            MealRecommendationCard(
                meal = meal,
                onSwapMeal = { loadAiMealPlan() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MacroItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MealRecommendationCard(
    meal: ScaledMeal,
    onSwapMeal: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meal.slot.label} • ${meal.portionLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = onSwapMeal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Ganti Menu AI", fontSize = 12.sp)
                }
            }

            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${meal.calories} kcal • P ${meal.proteinGrams}g • K ${meal.carbsGrams}g • L ${meal.fatGrams}g • ±${meal.prepMinutes} mnt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Bahan: ${meal.scaledIngredients.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )

            if (meal.instructions.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (expanded) "🔼 Sembunyikan Cara Memasak" else "👨‍🍳 Lihat Cara Memasak (${meal.instructions.size} Langkah)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Cara Pengolahan:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        meal.instructions.forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}