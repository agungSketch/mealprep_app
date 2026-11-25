package com.example.mealprep.ui

import androidx.compose.foundation.Image
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
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.rememberAppSettingsLauncher
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.ui.camera.PeekabooCamera
import com.preat.peekaboo.ui.camera.rememberPeekabooCameraState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun AiFoodScannerScreen(
    apiKey: String = "",
    activeMacroTarget: MacroTarget? = null,
    onBackToHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var userApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    val scannerService = remember(userApiKey) { GeminiFoodScannerService(apiKey = userApiKey) }
    val openAppSettings = rememberAppSettingsLauncher()

    var showCamera by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<FoodScanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraState = rememberPeekabooCameraState(
        onCapture = { bytes ->
            if (bytes != null) {
                selectedImageBytes = bytes
                scanResult = null
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
                scanResult = null
                errorMessage = null
            }
        }
    )

    // Mode Kamera Aktif (Full-screen / Area Kamera)
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
                    text = "📸 Foto Makanan",
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
                            text = "Aktifkan izin kamera di Pengaturan HP untuk dapat mengambil foto secara langsung.",
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

    // Halaman Utama Scanner & Hasil Analisis
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
                text = "📸 AI Food Scanner",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onBackToHome) {
                Text("Home")
            }
        }

        Text(
            text = "Jepret makanan langsung pakai kamera atau pilih dari galeri untuk estimasi kalori dan makro.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Preview Foto
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
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
                    contentDescription = "Preview makanan",
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
                    Text(text = "🍽️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada foto makanan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Gunakan tombol Kamera di bawah untuk memfoto makanan",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tombol Kamera & Galeri
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

        // Tombol Analisis Gemini AI
        if (selectedImageBytes != null) {
            Button(
                onClick = {
                    val imageBytes = selectedImageBytes ?: return@Button
                    isAnalyzing = true
                    errorMessage = null
                    scanResult = null

                    scope.launch {
                        val result = scannerService.analyzeFoodImage(
                            imageBytes = imageBytes,
                            mimeType = "image/jpeg"
                        )
                        isAnalyzing = false
                        result
                            .onSuccess { scanResult = it }
                            .onFailure { errorMessage = it.message ?: "Gagal menganalisis foto makanan." }
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
                    Text("Menganalisis dengan Gemini AI...")
                } else {
                    Text("Analisis Kalori & Makro", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        errorMessage?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = err,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        scanResult?.let { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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

                    if (activeMacroTarget != null) {
                        val remaining = activeMacroTarget.targetCalories - res.calories
                        Text(
                            text = "Sisa target kalori harianmu (${activeMacroTarget.targetCalories} kcal): $remaining kcal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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