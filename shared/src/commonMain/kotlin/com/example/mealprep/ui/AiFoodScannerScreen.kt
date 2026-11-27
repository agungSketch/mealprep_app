package com.example.mealprep.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealprep.data.FoodScanResult
import com.example.mealprep.data.GeminiFoodScannerService
import com.example.mealprep.data.RawIngredientScanResult
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.domain.RecommendedDayPlan
import com.example.mealprep.domain.ScaledMeal
import com.example.mealprep.rememberAppSettingsLauncher
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.ui.camera.PeekabooCamera
import com.preat.peekaboo.ui.camera.rememberPeekabooCameraState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap

enum class ScanMode {
    COOKED_FOOD,      // Makanan Matang
    RAW_INGREDIENTS   // Bahan Baku Mentah -> Racik Menu Harian
}

@Composable
fun AiFoodScannerScreen(
    apiKey: String = "",
    activeGoal: GoalType? = null,
    activeMacroTarget: MacroTarget? = null,
    onApplyPlanAndOpenRoutine: ((RecommendedDayPlan) -> Unit)? = null,
    onBackToHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var userApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    val scannerService = remember(userApiKey) { GeminiFoodScannerService(apiKey = userApiKey) }
    val openAppSettings = rememberAppSettingsLauncher()

    var scanMode by remember { mutableStateOf(ScanMode.RAW_INGREDIENTS) }
    var showCamera by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var cookedScanResult by remember { mutableStateOf<FoodScanResult?>(null) }
    var rawScanResult by remember { mutableStateOf<RawIngredientScanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraState = rememberPeekabooCameraState(
        onCapture = { bytes ->
            if (bytes != null) {
                selectedImageBytes = bytes
                cookedScanResult = null
                rawScanResult = null
                errorMessage = null
            }
            showCamera = false
        }
    )

    val galleryLauncher = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        resizeOptions = ResizeOptions(
            width = 640,
            height = 640,
            compressionQuality = 0.65
        ),
        onResult = { byteArrays ->
            val bytes = byteArrays.firstOrNull()
            if (bytes != null) {
                selectedImageBytes = bytes
                showCamera = false
                cookedScanResult = null
                rawScanResult = null
                errorMessage = null
            }
        }
    )

    // Mode Kamera Full-Screen / Preview Area
    if (showCamera) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (scanMode == ScanMode.RAW_INGREDIENTS) "📸 Foto Bahan Mentah" else "📸 Foto Makanan Matang",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { showCamera = false }) {
                    Text("Batal")
                }
            }

            PeekabooCamera(
                state = cameraState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                permissionDeniedContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "📷", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Izin Kamera Dibutuhkan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aktifkan izin kamera di Pengaturan HP untuk dapat mengambil foto bahan makanan secara langsung.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = openAppSettings,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("⚙️ Buka Pengaturan HP", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )

            Button(
                onClick = { cameraState.capture() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Ambil Foto", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    // Halaman Utama Scanner
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📸 AI Food & Recipe Scanner",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onBackToHome) {
                Text("Home")
            }
        }

        // Segmented Control Switch Mode (Bahan Mentah vs Makanan Matang)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = scanMode == ScanMode.RAW_INGREDIENTS,
                onClick = {
                    scanMode = ScanMode.RAW_INGREDIENTS
                    errorMessage = null
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("🥦 Bahan Mentah AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = scanMode == ScanMode.COOKED_FOOD,
                onClick = {
                    scanMode = ScanMode.COOKED_FOOD
                    errorMessage = null
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("🍽️ Makanan Matang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = if (scanMode == ScanMode.RAW_INGREDIENTS)
                "Foto bahan mentah di dapurmu (sayur, buah, daging, ikan, ayam, telur). AI akan mendeteksi bahan & meracik menu harian sehat."
            else
                "Foto makanan matang atau siap saji untuk mengestimasi kalori dan makronutrisinya secara instan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Preview Area Foto
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            val bytes = selectedImageBytes
            val imageBitmap = remember(bytes) { bytes?.decodeToImageBitmap() }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Preview foto bahan mentah / makanan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (scanMode == ScanMode.RAW_INGREDIENTS) "🥦🥩🍎" else "🍽️",
                        fontSize = 44.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (scanMode == ScanMode.RAW_INGREDIENTS) "Belum ada foto bahan mentah" else "Belum ada foto makanan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ambil foto bahan di meja dapur atau pilih dari galeri",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tombol Buka Kamera & Galeri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showCamera = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Buka Kamera", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { galleryLauncher.launch() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Galeri", fontWeight = FontWeight.SemiBold)
            }
        }

        // Tombol Proses AI Gemini
        if (selectedImageBytes != null) {
            Button(
                onClick = {
                    val bytes = selectedImageBytes ?: return@Button
                    isAnalyzing = true
                    errorMessage = null
                    cookedScanResult = null
                    rawScanResult = null

                    scope.launch {
                        if (scanMode == ScanMode.RAW_INGREDIENTS) {
                            val result = scannerService.analyzeRawIngredientsAndGeneratePlan(
                                imageBytes = bytes,
                                goal = activeGoal,
                                macroTarget = activeMacroTarget
                            )
                            isAnalyzing = false
                            result
                                .onSuccess { rawScanResult = it }
                                .onFailure { errorMessage = it.message ?: "Gagal memproses foto bahan mentah." }
                        } else {
                            val result = scannerService.analyzeFoodImage(
                                imageBytes = bytes
                            )
                            isAnalyzing = false
                            result
                                .onSuccess { cookedScanResult = it }
                                .onFailure { errorMessage = it.message ?: "Gagal menganalisis foto makanan." }
                        }
                    }
                },
                enabled = !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (scanMode == ScanMode.RAW_INGREDIENTS)
                            "Mendeteksi bahan & meracik menu AI..."
                        else
                            "Menganalisis kalori dengan Gemini AI..."
                    )
                } else {
                    Text(
                        text = if (scanMode == ScanMode.RAW_INGREDIENTS)
                            "✨ Olah Bahan Mentah Jadi Menu AI"
                        else
                            "Analisis Kalori Makanan Matang",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Pesan Error jika ada
        errorMessage?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = err,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Hasil Scan MODE 2: Bahan Mentah -> Day Plan
        rawScanResult?.let { rawResult ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🥦 Bahan Mentah Terdeteksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Badges Bahan Terdeteksi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rawResult.detectedIngredients.forEach { ing ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = ing,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = rawResult.chefNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Text(
                        text = "🍽️ Menu Olahan Harian AI (${rawResult.dayPlan.totalPlannedCalories} kcal)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    rawResult.dayPlan.meals.forEach { meal ->
                        RawIngredientMealCard(meal = meal)
                    }

                    if (onApplyPlanAndOpenRoutine != null) {
                        Button(
                            onClick = { onApplyPlanAndOpenRoutine(rawResult.dayPlan) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("📅 Gunakan Menu Ini ke Daily Routine", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Hasil Scan MODE 1: Makanan Matang
        cookedScanResult?.let { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = res.foodName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "${res.calories} kcal • Estimasi porsi ±${res.estimatedWeightGrams}g",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScanMacroItem(label = "Protein", value = "${res.proteinGrams}g")
                        ScanMacroItem(label = "Karbo", value = "${res.carbsGrams}g")
                        ScanMacroItem(label = "Lemak", value = "${res.fatGrams}g")
                    }

                    Text(
                        text = res.healthNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RawIngredientMealCard(meal: ScaledMeal) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = meal.slot.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${meal.calories} kcal • P ${meal.proteinGrams}g • K ${meal.carbsGrams}g • L ${meal.fatGrams}g • ±${meal.prepMinutes}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "🥦 Bahan: ${meal.scaledIngredients.joinToString(", ")}",
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
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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

@Composable
private fun ScanMacroItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
