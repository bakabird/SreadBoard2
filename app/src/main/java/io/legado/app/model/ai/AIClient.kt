package io.legado.app.model.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.AIProvider
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.getPrefBoolean
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import splitties.init.appCtx

class AIException(message: String, val code: Int) : IOException(message)

enum class AIErrorType {
    CLIENT, SERVER, NETWORK, UNKNOWN
}

data class AIErrorEvent(
    val title: String,
    val message: String,
    val type: AIErrorType,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

object AIClient {
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun generate(provider: AIProvider, messages: List<Map<String, String>>): String {
        val client = okHttpClient

        val endpoint = provider.baseUrl.trimEnd('/') + "/v1/chat/completions"
        val payload = mapOf(
            "model" to provider.model,
            "messages" to messages
        )

        val payloadJson = gson.toJson(payload)
        
        var logItem: AILogItem? = null
        if (appCtx.getPrefBoolean(PreferKey.aiInsightRequestPreview, false)) {
            logItem = AILogItem(
                id = UUID.randomUUID().toString(),
                time = System.currentTimeMillis(),
                method = "POST",
                url = endpoint,
                headers = if (provider.apiKey.isNotEmpty()) mapOf("Authorization" to "Bearer ***") else emptyMap(),
                requestBody = payloadJson
            )
            AILogManager.addLog(logItem)
        }

        val requestBody = payloadJson.toRequestBody(JSON)

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(requestBody)

        if (provider.apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer ${provider.apiKey}")
        }

        val request = requestBuilder.build()

        try {
            return client.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string() ?: ""
                
                logItem?.apply {
                    responseCode = response.code
                    responseBody = responseBodyString
                }

                if (!response.isSuccessful) {
                    logItem?.error = "HTTP ${response.code}"
                    val msg = if (responseBodyString.isNotBlank()) "Error ${response.code}: $responseBodyString" else "Error ${response.code}: ${response.message}"
                    throw AIException(msg, response.code)
                }

                val jsonObject = gson.fromJson(responseBodyString, JsonObject::class.java)

                val choices = jsonObject.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    val message = choices.get(0).asJsonObject.getAsJsonObject("message")
                    val content = message?.get("content")
                    if (content != null && !content.isJsonNull) {
                        content.asString
                    } else {
                        ""
                    }
                } else {
                    throw IOException("Invalid response structure: No choices found")
                }
            }
        } catch (e: Exception) {
            logItem?.error = e.toString()
            throw e
        }
    }
}
