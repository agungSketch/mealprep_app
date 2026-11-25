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

// 1. Model Hasil Analisis Makanan yang akan ditampilkan ke UI
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

// 2. DTO Request ke Gemini REST API
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

// 3. DTO Response dari Gemini REST API
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
            requestTimeoutMillis = 40_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 40_000
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

            // Menggunakan gemini-3.5-flash-lite untuk inferensi tercepat
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
            Result.failure(Exception(e.message ?: "Terjadi kesalahan saat menganalisis foto makanan."))
        }
    }
}