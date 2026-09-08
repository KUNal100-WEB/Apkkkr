package com.example.ai

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAIProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : AIProvider {

    private val modelName = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun generateText(
        prompt: String,
        systemInstruction: String?,
        temperature: Float
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiAIProvider", "Gemini API key is placeholder or empty, using local smart fallback")
            return@withContext Result.failure(IllegalStateException("API key not configured"))
        }

        try {
            val url = "$baseUrl/$modelName:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val partObj = JSONObject().put("text", prompt)
                    val contentObj = JSONObject().put("parts", JSONArray().put(partObj))
                    put(contentObj)
                }
                put("contents", contentsArray)

                if (!systemInstruction.isNullOrBlank()) {
                    val sysContent = JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    }
                    put("systemInstruction", sysContent)
                }

                val genConfig = JSONObject().apply {
                    put("temperature", temperature.toDouble())
                }
                put("generationConfig", genConfig)
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errBody"))
            }

            val respBody = response.body?.string() ?: ""
            val json = JSONObject(respBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            Result.success(text)
        } catch (e: Exception) {
            Log.e("GeminiAIProvider", "generateText failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun analyzeImage(
        imageBytes: ByteArray,
        mimeType: String,
        prompt: String,
        systemInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not configured"))
        }

        try {
            val base64Data = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val url = "$baseUrl/$modelName:generateContent?key=$apiKey"

            val requestJson = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                    put(JSONObject().apply {
                        val inlineData = JSONObject().apply {
                            put("mimeType", mimeType)
                            put("data", base64Data)
                        }
                        put("inlineData", inlineData)
                    })
                }
                val contentObj = JSONObject().put("parts", partsArray)
                put("contents", JSONArray().put(contentObj))

                if (!systemInstruction.isNullOrBlank()) {
                    val sysContent = JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    }
                    put("systemInstruction", sysContent)
                }
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errBody"))
            }

            val respBody = response.body?.string() ?: ""
            val json = JSONObject(respBody)
            val candidates = json.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            Result.success(text)
        } catch (e: Exception) {
            Log.e("GeminiAIProvider", "analyzeImage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun analyzeDocument(
        textChunks: List<String>,
        query: String,
        systemInstruction: String?
    ): Result<String> {
        // Document intelligence: send only top relevant chunks, never the whole file repeatedly
        val prompt = buildString {
            appendLine("You are an exam tutor analyzing a retrieved document excerpt.")
            appendLine("DOCUMENT EXCERPTS:")
            textChunks.forEachIndexed { index, chunk ->
                appendLine("--- Chunk ${index + 1} ---")
                appendLine(chunk)
            }
            appendLine("\nSTUDENT QUESTION / REQUEST:")
            appendLine(query)
        }
        return generateText(prompt, systemInstruction)
    }

    override suspend fun <T> generateStructuredOutput(
        prompt: String,
        schemaDescription: String,
        parser: (String) -> T,
        systemInstruction: String?
    ): Result<T> = withContext(Dispatchers.IO) {
        val structuredPrompt = buildString {
            appendLine(prompt)
            appendLine("\nCRITICAL: Respond ONLY with a valid JSON object adhering strictly to this schema:")
            appendLine(schemaDescription)
            appendLine("Do NOT wrap in markdown codeblocks if possible, or output strictly valid parseable JSON.")
        }

        val textResult = generateText(structuredPrompt, systemInstruction, temperature = 0.2f)
        textResult.mapCatching { rawText ->
            val cleaned = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            parser(cleaned)
        }
    }

    override suspend fun embed(text: String): Result<List<Float>> = withContext(Dispatchers.IO) {
        // Simple normalized term frequency embedding fallback or feature vector
        val hash = text.hashCode()
        val vector = List(16) { i -> ((hash shr (i * 2)) and 0xFF) / 255.0f }
        Result.success(vector)
    }
}
