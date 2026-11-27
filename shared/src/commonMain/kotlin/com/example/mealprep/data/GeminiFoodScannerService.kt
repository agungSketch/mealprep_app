package com.example.mealprep.data

import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.domain.MealSlot
import com.example.mealprep.domain.RecommendedDayPlan
import com.example.mealprep.domain.ScaledMeal
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// 1. Model Hasil Analisis Makanan Matang
@Serializable
data class FoodScanResult(
    val foodName: String,
    val estimatedWeightGrams: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val healthNote: String
)

// 2. Model Hasil Analisis Bahan Mentah & Olahan Menu Harian
data class RawIngredientScanResult(
    val detectedIngredients: List<String>,
    val chefNote: String,
    val dayPlan: RecommendedDayPlan
)

@Serializable
private data class RawIngredientPlanResponse(
    val detectedIngredients: List<String> = emptyList(),
    val chefNote: String = "",
    val meals: List<RawMealItem> = emptyList()
)

@Serializable
private data class RawMealItem(
    val slotName: String,
    val name: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val prepMinutes: Int = 15,
    val highlights: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList()
)

// DTO Request & Response ke Gemini REST API
@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
private data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
private data class GeminiGenerationConfig(
    val responseMimeType: String = "application/json"
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

class GeminiFoodScannerService(
    private val apiKey: String
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonParser)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 45_000
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analyzeFoodImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<FoodScanResult> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API Key belum diisi. Silakan masukkan Gemini API Key yang valid."))
        }

        return try {
            val base64Image = Base64.encode(imageBytes)

            val promptText = """
                Ahli gizi. Analisis foto makanan ini dan perkirakan porsinya.
                Kembalikan HANYA format JSON valid seperti ini:
                {
                  "foodName": "Nama makanan",
                  "estimatedWeightGrams": 250,
                  "calories": 450,
                  "proteinGrams": 25,
                  "carbsGrams": 50,
                  "fatGrams": 15,
                  "healthNote": "Catatan singkat 1 kalimat gizi makanan ini"
                }
            """.trimIndent()

            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = promptText),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = mimeType,
                                    data = base64Image
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json"
                )
            )

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=${apiKey.trim()}"

            val response: GeminiResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val rawJsonText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return Result.failure(Exception("Respons kosong dari Gemini AI."))

            val cleanJsonText = rawJsonText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedResult = jsonParser.decodeFromString<FoodScanResult>(cleanJsonText)
            Result.success(parsedResult)

        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Koneksi ke Gemini AI mengalami timeout. Pastikan koneksi internet stabil dan coba lagi."))
        } catch (e: ClientRequestException) {
            val code = e.response.status.value
            val errorDetail = try { e.response.body<String>() } catch (_: Exception) { "" }
            val message = if (code == 400 || code == 403) {
                "API Key Gemini tidak valid atau kuota habis (HTTP $code). $errorDetail"
            } else {
                "Gagal terhubung ke Gemini AI (Status HTTP $code). $errorDetail"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val friendlyMsg = if (msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("No address associated", ignoreCase = true) ||
                msg.contains("ConnectException", ignoreCase = true) ||
                msg.contains("UnresolvedAddressException", ignoreCase = true)
            ) {
                "Perangkat/emulator tidak terhubung ke internet. Pastikan koneksi internet aktif lalu coba lagi."
            } else {
                msg.ifBlank { "Terjadi kesalahan saat menganalisis foto makanan." }
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analyzeRawIngredientsAndGeneratePlan(
        imageBytes: ByteArray,
        goal: GoalType?,
        macroTarget: MacroTarget?,
        mimeType: String = "image/jpeg"
    ): Result<RawIngredientScanResult> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API Key belum diisi."))
        }

        return try {
            val base64Image = Base64.encode(imageBytes)
            val targetCalories = macroTarget?.targetCalories ?: 2000
            val goalLabel = goal?.label ?: "Maintain Sehat"
            val proteinTarget = macroTarget?.proteinGrams ?: 120
            val carbsTarget = macroTarget?.carbsGrams ?: 200
            val fatTarget = macroTarget?.fatGrams ?: 50

            val promptText = """
                Kamu adalah Chef Ahli Gizi Indonesia. Analisis foto bahan makanan mentah ini (seperti sayuran, buah, ikan, daging, ayam, telur, dll).
                1. Deteksi semua bahan mentah yang ada pada foto.
                2. Olah bahan mentah tersebut menjadi 3 menu makanan harian (BREAKFAST, LUNCH, DINNER) lengkap dengan langkah-langkah cara memasaknya yang lezat dan sehat untuk program $goalLabel (Target Harian: $targetCalories kcal, Protein: ${proteinTarget}g, Karbo: ${carbsTarget}g, Lemak: ${fatTarget}g).
                Kembalikan HANYA format JSON valid seperti ini:
                {
                  "detectedIngredients": ["150g dada ayam mentah", "100g brokoli", "2 butir telur"],
                  "chefNote": "Bahan mentah ini sangat kaya protein & serat, bagus untuk program sehat.",
                  "meals": [
                    {
                      "slotName":"BREAKFAST",
                      "name":"Oatmeal & Telur Rebus Brokoli",
                      "calories":450,
                      "proteinGrams":25,
                      "carbsGrams":50,
                      "fatGrams":14,
                      "prepMinutes":10,
                      "highlights":["Tinggi Serat"],
                      "ingredients":["2 butir telur","50g brokoli","50g oat"],
                      "instructions":["Kukus brokoli selama 3 menit hingga empuk.","Rebus telur hingga matang sedang.","Sajikan bersama oat hangat."]
                    },
                    {
                      "slotName":"LUNCH",
                      "name":"Tumis Ayam Brokoli Nasi Merah",
                      "calories":750,
                      "proteinGrams":45,
                      "carbsGrams":75,
                      "fatGrams":18,
                      "prepMinutes":20,
                      "highlights":["High Protein"],
                      "ingredients":["150g dada ayam mentah","100g brokoli","130g nasi merah"],
                      "instructions":["Potong dada ayam mentah dadu dan bumbui lada & bawang putih.","Tumis ayam dengan 1 sdt minyak zaitun hingga putih matang.","Masukkan brokoli, aduk cepat 3 menit.","Sajikan bersama nasi merah hangat."]
                    },
                    {
                      "slotName":"DINNER",
                      "name":"Sup Ayam Bening & Sayuran",
                      "calories":550,
                      "proteinGrams":35,
                      "carbsGrams":55,
                      "fatGrams":15,
                      "prepMinutes":20,
                      "highlights":["Rendah Lemak"],
                      "ingredients":["100g dada ayam mentah","50g brokoli","bumbu sup"],
                      "instructions":["Rebus air bersama jahe dan bawang putih geprek hingga mendidih.","Masukkan potongan dada ayam, masak hingga empuk.","Masukkan brokoli dan daun bawang, beri sedikit kaldu jamur lalu angkat."]
                    }
                  ]
                }
            """.trimIndent()

            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = promptText),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = mimeType,
                                    data = base64Image
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json"
                )
            )

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=${apiKey.trim()}"

            val response: GeminiResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val rawJsonText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return Result.failure(Exception("Respons kosong dari Gemini AI."))

            val cleanJsonText = rawJsonText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = jsonParser.decodeFromString<RawIngredientPlanResponse>(cleanJsonText)

            val scaledMeals = parsed.meals.mapIndexed { index, raw ->
                val slot = when (raw.slotName.uppercase()) {
                    "BREAKFAST" -> MealSlot.BREAKFAST
                    "LUNCH" -> MealSlot.LUNCH
                    "DINNER" -> MealSlot.DINNER
                    else -> MealSlot.entries.getOrElse(index % 3) { MealSlot.BREAKFAST }
                }
                ScaledMeal(
                    id = "raw_ai_${index}_${raw.slotName.lowercase()}",
                    name = raw.name,
                    slot = slot,
                    portionMultiplier = 1.0,
                    portionLabel = "Porsi AI (Bahan Mentah)",
                    calories = raw.calories,
                    proteinGrams = raw.proteinGrams,
                    carbsGrams = raw.carbsGrams,
                    fatGrams = raw.fatGrams,
                    prepMinutes = raw.prepMinutes,
                    highlights = raw.highlights.ifEmpty { listOf("Resep Bahan Mentah Foto") },
                    scaledIngredients = raw.ingredients,
                    instructions = raw.instructions
                )
            }

            val dayPlan = RecommendedDayPlan(
                targetCalories = targetCalories,
                totalPlannedCalories = scaledMeals.sumOf { it.calories },
                totalProteinGrams = scaledMeals.sumOf { it.proteinGrams },
                totalCarbsGrams = scaledMeals.sumOf { it.carbsGrams },
                totalFatGrams = scaledMeals.sumOf { it.fatGrams },
                meals = scaledMeals
            )

            val result = RawIngredientScanResult(
                detectedIngredients = parsed.detectedIngredients,
                chefNote = parsed.chefNote,
                dayPlan = dayPlan
            )

            Result.success(result)

        } catch (e: Exception) {
            val msg = e.message ?: ""
            val friendlyMsg = if (msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("No address associated", ignoreCase = true) ||
                msg.contains("ConnectException", ignoreCase = true) ||
                msg.contains("UnresolvedAddressException", ignoreCase = true)
            ) {
                "Perangkat/emulator tidak terhubung ke internet. Pastikan koneksi internet aktif lalu coba lagi."
            } else {
                msg.ifBlank { "Gagal memproses foto bahan mentah." }
            }
            Result.failure(Exception(friendlyMsg))
        }
    }
}
