package com.example.mealprep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealprep.domain.CalendarDayInfo
import com.example.mealprep.domain.DailyRoutineState
import com.example.mealprep.domain.DateUtils

@Composable
fun RoutineCalendarScreen(
    routineLogs: Map<String, DailyRoutineState>,
    todayIso: String = "2026-11-17",
    onSelectDate: (String) -> Unit,
    onBackToHome: () -> Unit
) {
    val (initialYear, initialMonth, _) = DateUtils.parseDateIso(todayIso)
    var currentYear by remember { mutableStateOf(initialYear) }
    var currentMonth by remember { mutableStateOf(initialMonth) }

    val monthGrid = remember(currentYear, currentMonth, todayIso) {
        DateUtils.generateMonthGrid(currentYear, currentMonth, todayIso)
    }

    // Hitung statistik bulanan
    val currentMonthLogs = routineLogs.filterKeys { dateIso ->
        val (y, m, _) = DateUtils.parseDateIso(dateIso)
        y == currentYear && m == currentMonth
    }

    val activeDaysCount = currentMonthLogs.values.count { it.consumedCalories > 0 || it.waterIntakeMl > 0 }
    val totalCaloriesMonth = currentMonthLogs.values.sumOf { it.consumedCalories }
    val goalAchievedDaysCount = currentMonthLogs.values.count { it.isDayGoalAchieved }

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
            Column {
                Text(
                    text = "📅 Kalender Jurnal Harian",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pilih tanggal untuk melihat atau mencatat rutinitas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onBackToHome,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Home", fontSize = 12.sp)
            }
        }

        // Ringkasan Statistik Bulanan
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Statistik Jurnal • ${DateUtils.getMonthName(currentMonth)} $currentYear",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalendarStatItem(label = "Hari Aktif", value = "$activeDaysCount Hari")
                    CalendarStatItem(label = "Target Tercapai", value = "$goalAchievedDaysCount Hari")
                    CalendarStatItem(label = "Total Kalori", value = "$totalCaloriesMonth kcal")
                }
            }
        }

        // Navigasi Bulan & Tahun (< November 2026 >)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth == 1) {
                            currentMonth = 12
                            currentYear -= 1
                        } else {
                            currentMonth -= 1
                        }
                    }
                ) {
                    Text("◀", fontSize = 14.sp)
                }

                Text(
                    text = "${DateUtils.getMonthName(currentMonth)} $currentYear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        if (currentMonth == 12) {
                            currentMonth = 1
                            currentYear += 1
                        } else {
                            currentMonth += 1
                        }
                    }
                ) {
                    Text("▶", fontSize = 14.sp)
                }
            }
        }

        // Header Hari (Sen, Sel, Rab, Kam, Jum, Sab, Min)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Grid Kalender 7 Kolom
        val chunkedGrid = monthGrid.chunked(7)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunkedGrid.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { dayInfo ->
                        val dayState = routineLogs[dayInfo.dateIso]
                        CalendarDayCard(
                            modifier = Modifier.weight(1f),
                            dayInfo = dayInfo,
                            routineState = dayState,
                            onClick = { onSelectDate(dayInfo.dateIso) }
                        )
                    }
                }
            }
        }

        // Quick button ke Jurnal Hari Ini
        Button(
            onClick = { onSelectDate(todayIso) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "📅 Buka Jurnal Hari Ini (${DateUtils.formatIndonesianDate(todayIso)})",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CalendarStatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun CalendarDayCard(
    modifier: Modifier,
    dayInfo: CalendarDayInfo,
    routineState: DailyRoutineState?,
    onClick: () -> Unit
) {
    val isCompleted = routineState?.isDayGoalAchieved == true
    val hasLog = (routineState?.consumedCalories ?: 0) > 0 || (routineState?.waterIntakeMl ?: 0) > 0

    val backgroundColor = when {
        dayInfo.isToday -> MaterialTheme.colorScheme.primaryContainer
        hasLog -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    }

    val contentAlpha = if (dayInfo.isCurrentMonth) 1.0f else 0.35f

    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable { onClick() }
            .then(
                if (dayInfo.isToday) Modifier.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${dayInfo.dayNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (dayInfo.isToday || hasLog) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )

            if (routineState != null && hasLog) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (routineState.consumedCalories > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "${routineState.consumedCalories}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
                            )
                        }
                    }

                    if (routineState.waterIntakeMl > 0) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
