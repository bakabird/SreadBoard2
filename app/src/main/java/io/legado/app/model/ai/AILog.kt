package io.legado.app.model.ai

import java.util.concurrent.ConcurrentLinkedDeque

data class AILogItem(
    val id: String,
    val time: Long,
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val requestBody: String,
    var responseCode: Int = 0,
    var responseBody: String? = null,
    var error: String? = null
)

object AILogManager {
    private const val MAX_LOGS = 50
    val logs = ConcurrentLinkedDeque<AILogItem>()

    fun addLog(item: AILogItem) {
        logs.addFirst(item)
        if (logs.size > MAX_LOGS) {
            logs.pollLast()
        }
    }

    fun clear() {
        logs.clear()
    }
}
