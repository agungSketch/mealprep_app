package com.example.mealprep.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealprep.domain.ActivityLevel
import com.example.mealprep.domain.CalorieCalculator
import com.example.mealprep.domain.Gender
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget

@Composable
fun CalorieCalculatorScreen(
    onBackToHome: () -> Unit,
    onCalculated: (goal: GoalType, result: MacroTarget) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var selectedActivity by remember { mutableStateOf(ActivityLevel.SEDENTARY) }
    var selectedGoal by remember { mutableStateOf(GoalType.MAINTAIN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header navigasi kembali ke Home
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Langkah 1 dari 2 • Input Data",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(
                onClick = onBackToHome,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Home", fontSize = 12.sp)
            }
        }

        Text(
            text = "🥗 Kalkulator Kalori Meal Prep",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Masukkan data tubuh dan target program Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = weight,
            onValueChange = {
                weight = it
                errorMessage = null
            },
            label = { Text("Berat Badan (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = height,
            onValueChange = {
                height = it
                errorMessage = null
            },
            label = { Text("Tinggi Badan (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = age,
            onValueChange = {
                age = it
                errorMessage = null
            },
            label = { Text("Usia (tahun)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Gender:", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(Gender.MALE to "Pria", Gender.FEMALE to "Wanita").forEach { (gender, label) ->
                val isSelected = selectedGender == gender
                Button(
                    onClick = { selectedGender = gender },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(label)
                }
            }
        }

        Text("Tingkat Aktivitas:", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val activityOptions = listOf(
                ActivityLevel.SEDENTARY to "Sangat Jarang (Kerja Kantoran)",
                ActivityLevel.LIGHT to "Ringan (Olahraga 1-3 hari/minggu)",
                ActivityLevel.MODERATE to "Sedang (Olahraga 3-5 hari/minggu)",
                ActivityLevel.ACTIVE to "Berat (Olahraga 6-7 hari/minggu)"
            )
            activityOptions.forEach { (level, label) ->
                val isSelected = selectedActivity == level
                OutlinedButton(
                    onClick = { selectedActivity = level },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(label, fontSize = 13.sp)
                }
            }
        }

        Text("Target Program:", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GoalType.entries.forEach { goal ->
                val isSelected = selectedGoal == goal
                OutlinedButton(
                    onClick = { selectedGoal = goal },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(goal.label, fontSize = 13.sp)
                }
            }
        }

        errorMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val w = weight.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                val a = age.toIntOrNull() ?: 0

                if (w > 0 && h > 0 && a > 0) {
                    val result = CalorieCalculator.calculateMacros(
                        weightKg = w,
                        heightCm = h,
                        ageYears = a,
                        gender = selectedGender,
                        activityLevel = selectedActivity,
                        goal = selectedGoal
                    )
                    onCalculated(selectedGoal, result)
                } else {
                    errorMessage = "Mohon isi berat badan, tinggi badan, dan usia dengan angka yang valid."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Hitung Kalori & Makro")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}