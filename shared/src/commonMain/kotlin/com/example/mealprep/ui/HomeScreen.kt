package com.example.mealprep.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealprep.domain.DailyRoutineState
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget

@Composable
fun HomeScreen(
    activeGoal: GoalType?,
    activeMacroTarget: MacroTarget?,
    routineState: DailyRoutineState? = null,
    onOpenCalculator: () -> Unit,
    onOpenMealResult: () -> Unit,
    onOpenAiScanner: () -> Unit,
    onOpenDailyRoutine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header sambutan
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Halo, Selamat Datang 👋",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "MealPrep AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Atur kebutuhan kalori harian, cek saran menu porsi dinamis, atau scan makananmu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Ringkasan target aktif jika user sudah pernah menghitung
        if (activeMacroTarget != null && activeGoal != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Aktif • ${activeGoal.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(
                            onClick = onOpenMealResult,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Lihat Menu", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "${activeMacroTarget.targetCalories} kcal / hari",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Protein ${activeMacroTarget.proteinGrams}g • Karbo ${activeMacroTarget.carbsGrams}g • Lemak ${activeMacroTarget.fatGrams}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    routineState?.let { state ->
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progres: ${state.consumedCalories} kcal • ${state.completedMealsCount} Meal • ${state.waterIntakeMl} ml Air",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(
                                onClick = onOpenDailyRoutine,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("Jurnal ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Belum ada target kalori tersimpan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Mulai dari Kalkulator Kalori untuk mendapatkan target harian dan saran menu katering yang disesuaikan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Fitur Utama",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Kartu fitur utama: Daily Routine & Jurnal Harian
        FeatureActionCard(
            emoji = "📅",
            title = "Rutinitas & Jurnal Harian",
            subtitle = "Pantau checklist makan harian, jurnal kalori, pencatat air minum 💧, dan kebiasaan sehat.",
            buttonLabel = "Buka Rutinitas Harian",
            onClick = onOpenDailyRoutine
        )

        // Kartu fitur 1: Kalkulator Kalori & Saran Menu
        FeatureActionCard(
            emoji = "🥗",
            title = "Kalkulator Kalori & Saran Menu",
            subtitle = "Hitung TDEE, target kalori/makro, lalu lihat daftar resep & porsi dinamis harian.",
            buttonLabel = "Buka Kalkulator",
            isSecondary = true,
            onClick = onOpenCalculator
        )

        // Kartu fitur 2: AI Food Scanner (tombol sendiri)
        FeatureActionCard(
            emoji = "📸",
            title = "AI Food Scanner",
            subtitle = "Foto makananmu untuk estimasi kalori dan makronutrisi secara instan.",
            buttonLabel = "Scan Makanan AI",
            isSecondary = true,
            onClick = onOpenAiScanner
        )
    }
}

@Composable
private fun FeatureActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    buttonLabel: String,
    isSecondary: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = emoji, fontSize = 32.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSecondary) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(buttonLabel, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(buttonLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}