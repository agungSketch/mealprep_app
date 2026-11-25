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
    val candidates: List<GeminiMealPlanCandidate>? = null
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
    val ingredients: List<String>
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
            return Result.failure(Exception("Gemini API Key belum diisi."))
        }

        return try {
            val promptText = """
                Ahli gizi Indonesia. Buatkan 3 menu harian (${macroTarget.targetCalories} kcal: BREAKFAST, LUNCH, DINNER) untuk program ${goal.label} (Target P:${macroTarget.proteinGrams}g, K:${macroTarget.carbsGrams}g, L:${macroTarget.fatGrams}g).
                Kembalikan HANYA format JSON valid seperti ini:
                {
                  "meals": [
                    {"slotName":"BREAKFAST","name":"Oatmeal Telur & Pisang","calories":450,"proteinGrams":25,"carbsGrams":50,"fatGrams":14,"prepMinutes":10,"highlights":["Tinggi Serat"],"ingredients":["60g oat","2 telur rebus"]},
                    {"slotName":"LUNCH","name":"Ayam Bakar Nasi Merah","calories":750,"proteinGrams":45,"carbsGrams":75,"fatGrams":18,"prepMinutes":20,"highlights":["High Protein"],"ingredients":["150g dada ayam","150g nasi merah"]},
                    {"slotName":"DINNER","name":"Ikan Dori Panggang","calories":600,"proteinGrams":35,"carbsGrams":55,"fatGrams":15,"prepMinutes":15,"highlights":["Low Fat"],"ingredients":["150g dori","150g kentang"]}
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

            // Menggunakan gemini-3.5-flash-lite untuk inferensi tercepat
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=${apiKey.trim()}"

            val response: GeminiMealPlanResponse = client.post(url) {
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
                    scaledIngredients = meal.ingredients
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

            Result.success(resultPlan)

        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Koneksi ke Gemini AI timeout. Silakan coba lagi."))
        } catch (e: ClientRequestException) {
            val code = e.response.status.value
            Result.failure(Exception("Gagal terhubung ke Gemini AI (Status HTTP $code)."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Gagal membuat menu makanan dengan Gemini AI."))
        }
    }
}