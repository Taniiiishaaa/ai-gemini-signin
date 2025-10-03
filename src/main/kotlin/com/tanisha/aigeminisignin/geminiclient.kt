package com.tanisha.aigeminisignin

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// ==== request/response payloads ====
data class Part(val text: String)
data class Content(val parts: List<Part>)
data class GeminiRequest(val contents: List<Content>)
data class Candidate(val content: Content)
data class GeminiResponse(val candidates: List<Candidate>)

data class Model(val name: String, val supportedGenerationMethods: List<String> = emptyList())
data class ListModelsResponse(val models: List<Model> = emptyList())

data class ErrorBody(val error: ErrorDetail)
data class ErrorDetail(val code: Int, val message: String)

// ==== client ====
class GeminiClient(private val apiKey: String) {
    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val base = "https://generativelanguage.googleapis.com"  // use v1 GA
    private val listModelsUrl = "$base/v1/models?key=$apiKey"

    fun testConnection(): String {
        val model = findWorkingModel() ?: return "Error: No model with generateContent available for this API key/project."
        val result = chat(model, "Say 'hello' if you can read this.")
        return if (result.startsWith("Error")) result else "Success: $result"
    }

    fun sendText(prompt: String): String {
        val model = findWorkingModel() ?: return "Error: No model with generateContent available for this API key/project."
        return chat(model, prompt)
    }

    fun chat(model: String, prompt: String): String {
        val url = "$base/v1/models/$model:generateContent?key=$apiKey"
        val bodyJson = gson.toJson(GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt))))))
        val request = Request.Builder().url(url).post(bodyJson.toRequestBody(json)).build()

        http.newCall(request).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (resp.isSuccessful) {
                val parsed = gson.fromJson(bodyStr, GeminiResponse::class.java)
                return parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "(no text)"
            } else {
                val errMsg = runCatching { gson.fromJson(bodyStr, ErrorBody::class.java).error.message }.getOrNull() ?: bodyStr
                return "Error ${resp.code}: ${resp.message}\n$errMsg"
            }
        }
    }

    fun chatStream(model: String, prompt: String, onChunk: (String) -> Unit) {
        val url = "$base/v1/models/$model:streamGenerateContent?key=$apiKey&alt=sse"
        val bodyJson = gson.toJson(GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt))))))
        val request = Request.Builder().url(url).post(bodyJson.toRequestBody(json)).build()

        try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "(no error body)"
                    val errMsg = runCatching { gson.fromJson(errorBody, ErrorBody::class.java).error.message }.getOrNull() ?: errorBody
                    onChunk("Error ${response.code}: ${response.message}\n$errMsg")
                    return
                }

                val source = response.body!!.source()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line()
                    if (line?.startsWith("data: ") == true) {
                        val data = line.substring(6)
                        try {
                            val parsed = gson.fromJson(data, GeminiResponse::class.java)
                            val text = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (text != null) {
                                onChunk(text)
                            }
                        } catch (e: Exception) {
                            // Ignore malformed JSON chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onChunk("Error: ${e.message}")
        }
    }

    /** Find a model that supports generateContent for THIS key/project. */
    fun findWorkingModel(): String? {
        val preferredModels = listOf(
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash",
            "gemini-1.5-pro-latest",
            "gemini-1.5-pro",
            "gemini-pro" // A common fallback
        )
        val models = listModels().associateBy { it.name.substringAfter("models/") }
        preferredModels.firstOrNull { name ->
            models[name]?.supportedGenerationMethods?.contains("generateContent") == true
        }?.let { return it }

        // Otherwise pick ANY model that supports generateContent
        return models.values.firstOrNull { it.supportedGenerationMethods.contains("generateContent") }
            ?.name?.substringAfter("models/")
    }

    private fun listModels(): List<Model> {
        val req = Request.Builder().url(listModelsUrl).get().build()
        http.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return emptyList()
            return runCatching { gson.fromJson(bodyStr, ListModelsResponse::class.java).models }.getOrDefault(emptyList())
        }
    }
}
