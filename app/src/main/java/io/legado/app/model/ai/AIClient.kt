package io.legado.app.model.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.AIRule
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.postEvent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import splitties.init.appCtx

data class AIRequestPreviewEvent(
    val requestId: String,
    val title: String,
    val message: String
)

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
    private val previewMutex = Mutex()
    private val previewWaiters = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    suspend fun generate(rule: AIRule, messages: List<Map<String, String>>): String {
        val client = okHttpClient

        val endpoint = rule.baseUrl.trimEnd('/') + "/v1/chat/completions"
        val payload = mapOf(
            "model" to rule.model,
            "messages" to messages
        )

        val payloadJson = gson.toJson(payload)
        awaitRequestPreviewIfEnabled(
            endpoint = endpoint,
            model = rule.model,
            hasAuthHeader = rule.apiKey.isNotEmpty(),
            payloadJson = payloadJson
        )

        val requestBody = payloadJson.toRequestBody(JSON)

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(requestBody)

        if (rule.apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer ${rule.apiKey}")
        }

        val request = requestBuilder.build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val msg = if (errorBody.isNotBlank()) "Error ${response.code}: $errorBody" else "Error ${response.code}: ${response.message}"
                throw AIException(msg, response.code)
            }

            val responseBody = response.body.string()
            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)

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
    }

    fun confirmRequestPreview(requestId: String) {
        previewWaiters.remove(requestId)?.complete(Unit)
    }

    private suspend fun awaitRequestPreviewIfEnabled(
        endpoint: String,
        model: String,
        hasAuthHeader: Boolean,
        payloadJson: String
    ) {
        if (!appCtx.getPrefBoolean(PreferKey.aiInsightRequestPreview, false)) return

        previewMutex.withLock {
            val requestId = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<Unit>()
            previewWaiters[requestId] = deferred

            val bodyBytes = payloadJson.toByteArray(Charsets.UTF_8).size
            val bodySha256 = sha256Hex(payloadJson)
            val maxChars = 40000
            val displayBody = if (payloadJson.length <= maxChars) {
                payloadJson
            } else {
                payloadJson.take(maxChars) + "\n... (truncated, total chars=${payloadJson.length})"
            }

            val message = buildString {
                append("Endpoint: ").append(endpoint).append('\n')
                append("Method: POST\n")
                append("Content-Type: application/json\n")
                append("Authorization: ").append(if (hasAuthHeader) "Bearer (set)" else "(none)").append('\n')
                append("Model: ").append(model).append('\n')
                append("Body bytes: ").append(bodyBytes).append('\n')
                append("Body SHA-256: ").append(bodySha256).append('\n')
                append('\n')
                append(displayBody)
            }

            postEvent(
                EventBus.AI_REQUEST_PREVIEW,
                AIRequestPreviewEvent(
                    requestId = requestId,
                    title = "AI Insight Request Preview",
                    message = message
                )
            )

            try {
                withTimeout(120_000) { deferred.await() }
            } catch (_: TimeoutCancellationException) {
            } finally {
                previewWaiters.remove(requestId)
            }
        }
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return buildString(bytes.size * 2) {
            for (b in bytes) {
                append(((b.toInt() ushr 4) and 0xF).toString(16))
                append((b.toInt() and 0xF).toString(16))
            }
        }
    }
}
