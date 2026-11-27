package com.example.mealprep.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget

data class RecentActivityItem(
    val id: String,
    val tag: String,
    val title: String,
    val calories: Int,
    val proteinGrams: Int,
    val subtitle: String
)

@Composable
fun HomeScreen(
    activeGoal: GoalType?,
    activeMacroTarget: MacroTarget?,
    todayCalories: Int = 0,
    todayProteinGrams: Int = 0,
    todayWaterMl: Int = 0,
    weeklyActiveDays: Int = 1,
    weeklyOnTrackDays: Int = 1,
    recentActivities: List<RecentActivityItem> = emptyList(),
    onOpenCalculator: () -> Unit,
    onOpenMealResult: () -> Unit,
    onOpenJournalCalendar: () -> Unit,
    onOpenTodayJournal: () -> Unit,
    onOpenAiScanner: () -> Unit,
    onOpenIngredientRecipe: () -> Unit = onOpenAiScanner
) {
    var showShareModal by remember { mutableStateOf(false) }

    val targetCal = activeMacroTarget?.targetCalories ?: 1900
    val targetPro = activeMacroTarget?.proteinGrams ?: 140
    val calProgress = (todayCalories.toFloat() / targetCal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val proProgress = (todayProteinGrams.toFloat() / targetPro.coerceAtLeast(1)).coerceIn(0f, 1f)

    // Dialog Share Preview Pencapaian ala Strava
    if (showShareModal) {
        AlertDialog(
            onDismissRequest = { showShareModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏆", fontSize = 24.sp)
                    Text(
                        text = "Bagikan Pencapaian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Preview Kartu Pencapaian Nutrisi (Siap Bagikan ke Story IG / WhatsApp):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                                    text = "GRAMPULSE AI",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ON-TRACK 🏆",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "Progres Nutrisi Harian",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Program", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        activeGoal?.label ?: "Program Sehat",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Asupan Kalori", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "$todayCalories / $targetCal kcal",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Protein: ${todayProteinGrams}g / ${targetPro}g", style = MaterialTheme.typography.bodySmall)
                                Text("Air: $todayWaterMl ml", style = MaterialTheme.typography.bodySmall)
                            }

                            Text(
                                text = "\"Konsistensi adalah kunci utama transformasi bentuk tubuh!\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showShareModal = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Avatar + Brand + Tombol Bagikan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "GP",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = "GRAMPULSE AI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeGoal?.label ?: "Belum Set Target",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalButton(
                onClick = { showShareModal = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Bagikan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Ringkasan Mingguan (ala "My week")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                    Text(
                        text = "Minggu Ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onOpenJournalCalendar,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Buka Kalender", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeeklyStatColumn(label = "Hari Aktif", value = "$weeklyActiveDays hr")
                    WeeklyStatColumn(label = "On-Track", value = "$weeklyOnTrackDays hr")
                    WeeklyStatColumn(label = "Air Hari Ini", value = "${todayWaterMl} ml")
                }
            }
        }

        // Dua Kartu Metrik Utama (Kalori & Protein)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricHighlightCard(
                modifier = Modifier.weight(1f),
                title = "Kalori Masuk",
                mainValue = "$todayCalories",
                subValue = "Target $targetCal kcal",
                progress = calProgress,
                onClick = if (activeMacroTarget != null) onOpenMealResult else onOpenCalculator
            )

            MetricHighlightCard(
                modifier = Modifier.weight(1f),
                title = "Protein",
                mainValue = "${todayProteinGrams}g",
                subValue = "Target ${targetPro}g",
                progress = proProgress,
                onClick = onOpenTodayJournal
            )
        }

        // Banner Quick Action (Scan Bahan -> Resep / Kalkulator)
        Surface(
            onClick = if (activeMacroTarget != null) onOpenIngredientRecipe else onOpenCalculator,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (activeMacroTarget != null) {
                            "Foto Bahan Makanan → Resep"
                        } else {
                            "Hitung Kalori & Makro Pertamamu"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (activeMacroTarget != null) {
                            "Racik menu tinggi protein sesuai target $targetCal kcal."
                        } else {
                            "Tentukan BMR, TDEE, dan target Cutting / Maintain / Bulk."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = if (activeMacroTarget != null) onOpenIngredientRecipe else onOpenCalculator,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (activeMacroTarget != null) "Racik" else "Mulai",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Log & Menu Terbaru (ala "Recent activities")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktivitas & Menu Terbaru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onOpenAiScanner,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text("+ Scan AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        val itemsToShow = recentActivities.ifEmpty {
            listOf(
                RecentActivityItem(
                    id = "default_1",
                    tag = "MEAL PLAN",
                    title = "Menu Porsi Dinamis",
                    calories = targetCal,
                    proteinGrams = targetPro,
                    subtitle = "Sesuai target harianmu"
                ),
                RecentActivityItem(
                    id = "default_2",
                    tag = "AI SCANNER",
                    title = "Cek Kalori Foto",
                    calories = 0,
                    proteinGrams = 0,
                    subtitle = "Scan makanan matang / bahan"
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsToShow.forEach { item ->
                Card(
                    modifier = Modifier.width(230.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (item.calories > 0) {
                            Text(
                                text = "${item.calories} kcal • P ${item.proteinGrams}g",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WeeklyStatColumn(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricHighlightCard(
    modifier: Modifier = Modifier,
    title: String,
    mainValue: String,
    subValue: String,
    progress: Float,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = mainValue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}
