package com.example.mealprep.data

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

@Serializable
data class GeneratedIngredientRecipe(
    val recipeName: String,
    val detectedIngredients: List<String>,
    val prepMinutes: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val ingredientsWithAmount: List<String>,
    val cookingSteps: List<String>
)

@Serializable
private data class IngredientGeminiRequest(
    val contents: List<IngredientGeminiContent>,
    val generationConfig: IngredientGeminiConfig
)

@Serializable
private data class IngredientGeminiContent(
    val parts: List<IngredientGeminiPart>
)

@Serializable
private data class IngredientGeminiPart(
    val text: String? = null,
    val inlineData: IngredientGeminiInlineData? = null
)

@Serializable
private data class IngredientGeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
private data class IngredientGeminiConfig(
    val responseMimeType: String = "application/json"
)

@Serializable
private data class IngredientGeminiResponse(
    val candidates: List<IngredientGeminiCandidate>? = null,
    val error: IngredientGeminiErrorDto? = null
)

@Serializable
private data class IngredientGeminiErrorDto(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
private data class IngredientGeminiCandidate(
    val content: IngredientGeminiContent? = null
)

class GeminiIngredientRecipeService(
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

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun generateRecipeFromPhoto(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        targetCalories: Int? = null,
        targetProteinGrams: Int? = null
    ): Result<GeneratedIngredientRecipe> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API Key belum diisi."))
        }

        return try {
            val base64Image = Base64.encode(imageBytes)
            val targetContext = if (targetCalories != null && targetProteinGrams != null) {
                "Sesuaikan porsi resep agar mendukung target harian sekitar $targetCalories kcal dan protein $targetProteinGrams g/hari untuk pegiat gym/olahraga."
            } else {
                "Utamakan resep praktis, bergizi seimbang, dan cukup tinggi protein untuk pegiat gym/olahraga."
            }

            val prompt = """
                Kamu adalah chef meal prep & ahli nutrisi olahraga.
                Identifikasi bahan makanan yang terlihat di foto ini, lalu buat 1 resep praktis yang memakai bahan-bahan tersebut.
                $targetContext
                Kembalikan HANYA JSON valid dengan struktur persis berikut:
                {
                  "recipeName": "Nama resep",
                  "detectedIngredients": ["bahan 1", "bahan 2"],
                  "prepMinutes": 15,
                  "calories": 520,
                  "proteinGrams": 42,
                  "carbsGrams": 50,
                  "fatGrams": 15,
                  "ingredientsWithAmount": ["150g dada ayam", "100g nasi merah"],
                  "cookingSteps": ["Langkah 1 singkat", "Langkah 2 singkat"]
                }
            """.trimIndent()

            val body = IngredientGeminiRequest(
                contents = listOf(
                    IngredientGeminiContent(
                        parts = listOf(
                            IngredientGeminiPart(text = prompt),
                            IngredientGeminiPart(
                                inlineData = IngredientGeminiInlineData(
                                    mimeType = mimeType,
                                    data = base64Image
                                )
                            )
                        )
                    )
                ),
                generationConfig = IngredientGeminiConfig(responseMimeType = "application/json")
            )

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey.trim()}"
            val response: IngredientGeminiResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()

            response.error?.let { err ->
                val msg = err.message ?: "Error HTTP ${err.code}"
                return Result.failure(Exception("Gemini AI Error: $msg"))
            }

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return Result.failure(Exception("Respons kosong dari Gemini AI"))

            val cleanJsonText = rawJson
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = jsonParser.decodeFromString<GeneratedIngredientRecipe>(cleanJsonText)
            Result.success(parsed)
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Koneksi ke Gemini AI timeout. Silakan periksa koneksi internet lalu coba lagi."))
        } catch (e: ClientRequestException) {
            val code = e.response.status.value
            Result.failure(Exception("Gagal terhubung ke Gemini AI (Status HTTP $code)."))
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val friendlyMsg = if (msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("No address associated", ignoreCase = true) ||
                msg.contains("ConnectException", ignoreCase = true) ||
                msg.contains("UnresolvedAddressException", ignoreCase = true)
            ) {
                "Perangkat/emulator tidak terhubung ke internet. Pastikan koneksi internet aktif lalu coba lagi."
            } else {
                msg.ifBlank { "Gagal memproses foto bahan makanan." }
            }
            Result.failure(Exception(friendlyMsg))
        }
    }
}
