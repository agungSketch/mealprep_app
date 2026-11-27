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

@Serializable
private data class GeminiMealPlanRequest(
    val contents: List<GeminiMealPlanContent>,
    val generationConfig: GeminiMealPlanGenerationConfig
)

@Serializable
private data class GeminiMealPlanContent(
    val parts: List<GeminiMealPlanPart>
)

@Serializable
private data class GeminiMealPlanPart(
    val text: String
)

@Serializable
private data class GeminiMealPlanGenerationConfig(
    val responseMimeType: String = "application/json"
)

@Serializable
private data class GeminiMealPlanResponse(
    val candidates: List<GeminiMealPlanCandidate>? = null,
    val error: MealPlannerGeminiErrorDto? = null
)

@Serializable
private data class MealPlannerGeminiErrorDto(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
private data class GeminiMealPlanCandidate(
    val content: GeminiMealPlanContentResponse? = null
)

@Serializable
private data class GeminiMealPlanContentResponse(
    val parts: List<GeminiMealPlanPartResponse>? = null
)

@Serializable
private data class GeminiMealPlanPartResponse(
    val text: String? = null
)

@Serializable
data class AiDayPlanDto(
    val meals: List<AiMealDto>
)

@Serializable
data class AiMealDto(
    val slotName: String,
    val name: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val prepMinutes: Int,
    val highlights: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList()
)

class GeminiMealPlannerService(
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
            requestTimeoutMillis = 40_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 40_000
        }
    }

    suspend fun generateAiMealPlan(
        goal: GoalType,
        macroTarget: MacroTarget
    ): Result<RecommendedDayPlan> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API Key belum diisi. Silakan isi GEMINI_API_KEY di file .env terlebih dahulu."))
        }

        val promptText = """
            Kamu adalah chef meal prep & ahli gizi olahraga Indonesia.
            Buatkan 3 racikan menu harian lengkap (BREAKFAST, LUNCH, DINNER) yang lezat beserta takaran bahan dan langkah-langkah cara memasaknya untuk program ${goal.label} (Target Harian: ${macroTarget.targetCalories} kcal, Protein: ${macroTarget.proteinGrams}g, Karbo: ${macroTarget.carbsGrams}g, Lemak: ${macroTarget.fatGrams}g).
            Kembalikan HANYA JSON valid dengan struktur persis seperti berikut:
            {
              "meals": [
                {
                  "slotName":"BREAKFAST",
                  "name":"Oatmeal Telur & Pisang",
                  "calories":450,
                  "proteinGrams":25,
                  "carbsGrams":50,
                  "fatGrams":14,
                  "prepMinutes":10,
                  "highlights":["Tinggi Serat"],
                  "ingredients":["60g oat","2 telur rebus","1 pisang"],
                  "instructions":["Seduh oat dengan air/susu hangat.","Rebus telur 8 menit hingga matang.","Potong pisang dan sajikan bersama."]
                },
                {
                  "slotName":"LUNCH",
                  "name":"Ayam Bakar Nasi Merah",
                  "calories":750,
                  "proteinGrams":45,
                  "carbsGrams":75,
                  "fatGrams":18,
                  "prepMinutes":20,
                  "highlights":["High Protein"],
                  "ingredients":["150g dada ayam","150g nasi merah","100g brokoli"],
                  "instructions":["Bumbui ayam dengan kecap manis & lada.","Panggang ayam di pan antilengket.","Kukus brokoli dan sajikan dengan nasi merah."]
                },
                {
                  "slotName":"DINNER",
                  "name":"Ikan Dori Panggang Lemon",
                  "calories":600,
                  "proteinGrams":35,
                  "carbsGrams":55,
                  "fatGrams":15,
                  "prepMinutes":15,
                  "highlights":["Low Fat"],
                  "ingredients":["150g dori","150g kentang rebus"],
                  "instructions":["Lumuri ikan dori dengan perasan lemon & lada.","Panggang dori hingga matang merata.","Rebus kentang dan sajikan hangat."]
                }
              ]
            }
        """.trimIndent()

        val requestBody = GeminiMealPlanRequest(
            contents = listOf(
                GeminiMealPlanContent(
                    parts = listOf(GeminiMealPlanPart(text = promptText))
                )
            ),
            generationConfig = GeminiMealPlanGenerationConfig(
                responseMimeType = "application/json"
            )
        )

        val candidateUrls = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=${apiKey.trim()}",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=${apiKey.trim()}",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=${apiKey.trim()}",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey.trim()}",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=${apiKey.trim()}",
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=${apiKey.trim()}"
        )

        var lastException: Exception? = null

        for (url in candidateUrls) {
            try {
                val response: GeminiMealPlanResponse = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body()

                if (response.error != null) {
                    val errCode = response.error.code ?: 0
                    val errMsg = response.error.message ?: ""
                    if (errCode == 404 || errMsg.contains("is not found", ignoreCase = true)) {
                        lastException = Exception("Gemini AI Error ($errCode): $errMsg")
                        continue
                    }
                    return Result.failure(Exception("Gemini AI Error: $errMsg"))
                }

                val rawJsonText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?: continue

                val cleanJsonText = rawJsonText
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val aiPlan = jsonParser.decodeFromString<AiDayPlanDto>(cleanJsonText)

                val scaledMeals = aiPlan.meals.mapIndexed { index, meal ->
                    val slot = when (meal.slotName.uppercase()) {
                        "BREAKFAST" -> MealSlot.BREAKFAST
                        "LUNCH" -> MealSlot.LUNCH
                        "DINNER" -> MealSlot.DINNER
                        else -> when (index) {
                            0 -> MealSlot.BREAKFAST
                            1 -> MealSlot.LUNCH
                            else -> MealSlot.DINNER
                        }
                    }

                    ScaledMeal(
                        id = "ai_meal_$index",
                        name = meal.name,
                        slot = slot,
                        portionMultiplier = 1.0,
                        portionLabel = "Porsi AI",
                        calories = meal.calories,
                        proteinGrams = meal.proteinGrams,
                        carbsGrams = meal.carbsGrams,
                        fatGrams = meal.fatGrams,
                        prepMinutes = meal.prepMinutes,
                        highlights = meal.highlights.ifEmpty { listOf("Resep AI") },
                        scaledIngredients = meal.ingredients,
                        instructions = meal.instructions
                    )
                }

                val resultPlan = RecommendedDayPlan(
                    targetCalories = macroTarget.targetCalories,
                    totalPlannedCalories = scaledMeals.sumOf { it.calories },
                    totalProteinGrams = scaledMeals.sumOf { it.proteinGrams },
                    totalCarbsGrams = scaledMeals.sumOf { it.carbsGrams },
                    totalFatGrams = scaledMeals.sumOf { it.fatGrams },
                    meals = scaledMeals
                )

                return Result.success(resultPlan)

            } catch (e: HttpRequestTimeoutException) {
                lastException = Exception("Koneksi ke Gemini AI timeout. Silakan periksa koneksi internet lalu coba lagi.")
            } catch (e: ClientRequestException) {
                val code = e.response.status.value
                val errorDetail = try { e.response.body<String>() } catch (_: Exception) { "" }
                lastException = Exception("Gagal terhubung ke Gemini AI (Status HTTP $code). $errorDetail")
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val friendlyMsg = if (msg.contains("Unable to resolve host", ignoreCase = true) ||
                    msg.contains("No address associated", ignoreCase = true) ||
                    msg.contains("ConnectException", ignoreCase = true) ||
                    msg.contains("UnresolvedAddressException", ignoreCase = true)
                ) {
                    "Perangkat/emulator tidak terhubung ke internet. Pastikan koneksi internet aktif lalu klik 'Coba Lagi'."
                } else {
                    msg.ifBlank { "Gagal meracik menu makanan dengan Gemini AI." }
                }
                lastException = Exception(friendlyMsg)
            }
        }

        return Result.failure(lastException ?: Exception("Gagal terhubung ke Gemini AI."))
    }
}
