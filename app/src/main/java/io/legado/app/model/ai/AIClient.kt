package io.legado.app.model.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.legado.app.data.entities.AIRule
import io.legado.app.help.http.okHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object AIClient {
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun generate(rule: AIRule, messages: List<Map<String, String>>): String {
        // Create a new client sharing the connection pool but with potential timeouts if needed, 
        // though default okHttpClient has good timeouts.
        val client = okHttpClient
        
        val payload = mapOf(
            "model" to rule.model,
            "messages" to messages
        )
        
        val requestBody = gson.toJson(payload).toRequestBody(JSON)
        
        val requestBuilder = Request.Builder()
            .url(rule.baseUrl.trimEnd('/') + "/v1/chat/completions") // Assuming OpenAI compatible
            .post(requestBody)

        if (rule.apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer ${rule.apiKey}")
        }

        val request = requestBuilder.build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                
                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                
                val choices = jsonObject.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                     choices.get(0).asJsonObject
                        .getAsJsonObject("message")
                        .get("content").asString
                } else {
                    throw IOException("Invalid response structure: No choices found")
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
