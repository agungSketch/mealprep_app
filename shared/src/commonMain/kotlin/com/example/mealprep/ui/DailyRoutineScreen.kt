package com.example.mealprep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealprep.data.GeminiMealPlannerService
import com.example.mealprep.data.SavedMealPlanTemplate
import com.example.mealprep.domain.DailyMealCheck
import com.example.mealprep.domain.DailyRoutineState
import com.example.mealprep.domain.DateUtils
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.domain.MealRecommendationEngine
import com.example.mealprep.domain.RecommendedDayPlan
import kotlinx.coroutines.launch

@Composable
fun DailyRoutineScreen(
    routineState: DailyRoutineState,
    savedTemplates: List<SavedMealPlanTemplate> = emptyList(),
    apiKey: String = "",
    onUpdateRoutineState: (DailyRoutineState) -> Unit,
    onApplyPlanToDate: (String, RecommendedDayPlan) -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenMealResult: () -> Unit,
    onBackToCalendar: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val plannerService = remember(apiKey) { GeminiMealPlannerService(apiKey = apiKey) }

    val macroTarget = routineState.macroTarget
    val goal = routineState.activeGoal ?: GoalType.MAINTAIN
    val targetCalories = macroTarget?.targetCalories ?: 2000

    val calorieProgress = (routineState.consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f)
    val remainingCalories = targetCalories - routineState.consumedCalories

    var showChangeMenuDialog by remember { mutableStateOf(false) }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog Pilihan / Ganti Menu untuk Tanggal Ini
    if (showChangeMenuDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGeneratingAi) showChangeMenuDialog = false },
            title = {
                Text(
                    text = "🍽️ Pilihan Menu untuk ${DateUtils.formatIndonesianDate(routineState.dateIso)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih cara menentukan menu untuk tanggal ini:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Error Message jika AI gagal
                    aiErrorMessage?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = err,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Pilihan 1: Generate Menu Baru dengan Gemini AI (Online)
                    Button(
                        onClick = {
                            if (macroTarget == null) {
                                onOpenCalculator()
                                showChangeMenuDialog = false
                                return@Button
                            }
                            isGeneratingAi = true
                            aiErrorMessage = null

                            scope.launch {
                                val result = plannerService.generateAiMealPlan(goal, macroTarget)
                                isGeneratingAi = false
                                result
                                    .onSuccess { aiPlan ->
                                        onApplyPlanToDate(routineState.dateIso, aiPlan)
                                        showChangeMenuDialog = false
                                    }
                                    .onFailure { err ->
                                        aiErrorMessage = err.message ?: "Gagal meracik menu AI. Silakan gunakan variasi menu lokal."
                                    }
                            }
                        },
                        enabled = !isGeneratingAi,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini AI Sedang Meracik...")
                        } else {
                            Text("✨ Racik Menu Baru dengan Gemini AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Pilihan 2: Acak Variasi Menu Lokal (0 Token AI)
                    OutlinedButton(
                        onClick = {
                            val currentIds = routineState.mealChecks.map { it.meal.id }.toSet()
                            val target = macroTarget ?: MacroTarget(2000, 2000, 120, 200, 50)
                            val randomizedPlan = MealRecommendationEngine.generateDayPlan(
                                macroTarget = target,
                                excludeIds = currentIds,
                                randomize = true
                            )
                            onApplyPlanToDate(routineState.dateIso, randomizedPlan)
                            showChangeMenuDialog = false
                        },
                        enabled = !isGeneratingAi,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🎲 Acak Variasi Menu (Lokal Engine)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "📚 Koleksi Menu Tersimpan (0 Token AI):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (savedTemplates.isEmpty()) {
                        Text(
                            text = "Belum ada koleksi menu tersimpan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            savedTemplates.forEach { tpl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isGeneratingAi) {
                                            onApplyPlanToDate(routineState.dateIso, tpl.plan)
                                            showChangeMenuDialog = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = tpl.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = tpl.plan.meals.joinToString(" • ") { it.name },
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showChangeMenuDialog = false },
                    enabled = !isGeneratingAi
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📅 Rutinitas & Jurnal Harian",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = DateUtils.formatIndonesianDate(routineState.dateIso),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedButton(
                onClick = onBackToCalendar,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Kalender 📅", fontSize = 12.sp)
            }
        }

        // Ringkasan Kalori & Progress Harian
        if (macroTarget != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Asupan Kalori Hari Ini",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${routineState.consumedCalories} / $targetCalories kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    LinearProgressIndicator(
                        progress = { calorieProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (remainingCalories >= 0) "Sisa: $remainingCalories kcal" else "Surplus: ${-remainingCalories} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Target Goal: ${routineState.activeGoal?.label ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RoutineMacroProgress(
                            label = "Protein",
                            consumed = routineState.consumedProtein,
                            target = macroTarget.proteinGrams
                        )
                        RoutineMacroProgress(
                            label = "Karbo",
                            consumed = routineState.consumedCarbs,
                            target = macroTarget.carbsGrams
                        )
                        RoutineMacroProgress(
                            label = "Lemak",
                            consumed = routineState.consumedFat,
                            target = macroTarget.fatGrams
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Target Kalori Belum Ditetapkan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Hitung dulu target kalori harianmu agar rutinitas dapat disesuaikan dengan kebutuhan tubuhmu secara presisi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = onOpenCalculator,
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Hitung Kalori Sekarang", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section Menu Harian / Meal Journal Checklist
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🍽️ Jurnal Makan Tanggal Ini (${routineState.completedMealsCount}/${routineState.mealChecks.size.coerceAtLeast(3)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showChangeMenuDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("🔀 Ganti Menu Tanggal Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (routineState.mealChecks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🥗", fontSize = 36.sp)
                        Text(
                            text = "Belum Ada Menu Terjadwal untuk Tanggal Ini",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Pilih menu dari koleksi tersimpan atau buat racikan menu baru untuk tanggal ini.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showChangeMenuDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pilih / Ganti Menu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onOpenMealResult,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Menu AI Gemini", fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                routineState.mealChecks.forEachIndexed { index, mealCheck ->
                    RoutineMealItemCard(
                        mealCheck = mealCheck,
                        onToggle = { isChecked ->
                            val updatedChecks = routineState.mealChecks.toMutableList()
                            updatedChecks[index] = mealCheck.copy(isCompleted = isChecked)
                            onUpdateRoutineState(routineState.copy(mealChecks = updatedChecks))
                        }
                    )
                }
            }
        }

        // Section Water Tracker
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💧", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Pencatat Air Minum",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target: ${routineState.waterTargetMl} ml (±10 gelas)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${routineState.waterIntakeMl} ml",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val waterProgress = (routineState.waterIntakeMl.toFloat() / routineState.waterTargetMl.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { waterProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val newIntake = routineState.waterIntakeMl + 250
                            onUpdateRoutineState(routineState.copy(waterIntakeMl = newIntake))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+250 ml (1 Gelas)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val newIntake = routineState.waterIntakeMl + 500
                            onUpdateRoutineState(routineState.copy(waterIntakeMl = newIntake))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+500 ml", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateRoutineState(routineState.copy(waterIntakeMl = 0))
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Reset", fontSize = 11.sp)
                    }
                }
            }
        }

        // Section Kebiasaan Sehat / Habits Checklist
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🏃‍♂️ Kebiasaan Sehat Harian (${routineState.completedHabitsCount}/${routineState.habits.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            routineState.habits.forEachIndexed { index, habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val updatedHabits = routineState.habits.toMutableList()
                            updatedHabits[index] = habit.copy(isCompleted = !habit.isCompleted)
                            onUpdateRoutineState(routineState.copy(habits = updatedHabits))
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (habit.isCompleted)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = habit.isCompleted,
                            onCheckedChange = { isChecked ->
                                val updatedHabits = routineState.habits.toMutableList()
                                updatedHabits[index] = habit.copy(isCompleted = isChecked)
                                onUpdateRoutineState(routineState.copy(habits = updatedHabits))
                            }
                        )
                        Text(text = habit.icon, fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (habit.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RoutineMacroProgress(
    label: String,
    consumed: Int,
    target: Int
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = "$consumed / ${target}g",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun RoutineMealItemCard(
    mealCheck: DailyMealCheck,
    onToggle: (Boolean) -> Unit
) {
    val meal = mealCheck.meal
    var expandedInstructions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!mealCheck.isCompleted) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mealCheck.isCompleted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = mealCheck.isCompleted,
                        onCheckedChange = onToggle
                    )
                    Text(
                        text = meal.slot.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (mealCheck.isCompleted) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = "✓ Dimakan",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = if (mealCheck.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.padding(start = 40.dp)
            )

            Text(
                text = "${meal.calories} kcal • P ${meal.proteinGrams}g • K ${meal.carbsGrams}g • L ${meal.fatGrams}g • (${meal.portionLabel})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 40.dp)
            )

            if (meal.scaledIngredients.isNotEmpty()) {
                Text(
                    text = "🥦 Bahan: ${meal.scaledIngredients.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }

            if (meal.instructions.isNotEmpty()) {
                TextButton(
                    onClick = { expandedInstructions = !expandedInstructions },
                    modifier = Modifier.padding(start = 32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (expandedInstructions) "🔼 Sembunyikan Cara Memasak" else "👨‍🍳 Lihat Cara Memasak (${meal.instructions.size} Langkah)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (expandedInstructions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp)
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
