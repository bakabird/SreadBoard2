package io.legado.app.model.ai

import io.legado.app.utils.DebugLog
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ChapterInsight
import io.legado.app.help.book.BookHelp
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.constant.EventBus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import splitties.init.appCtx
import java.util.concurrent.ConcurrentHashMap

object InsightManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val queue = ConcurrentHashMap<String, TaskEntry>() // Key: bookUrl-chapterIndex-type
    private val tasksFlow = MutableStateFlow<List<AITask>>(emptyList())

    const val FEATURE_SUMMARY = "summary"
    const val FEATURE_SKIP_RISK = "skip_risk"
    const val SKIP_RISK_ENABLED = false

    // Status constants matching ChapterInsight
    const val STATUS_NONE = 0
    const val STATUS_GENERATING = 1
    const val STATUS_READY = 2
    const val STATUS_FAILED = 3

    val DEFAULT_SUMMARY_PROMPT = """
        You are a helpful reading assistant.
        Summarize the following chapter in about 200 words.
        Provide 5-10 bullet points for key events.
        If the chapter introduces new concepts or new character relationships, mark them with [NEW CONCEPT] or [NEW RELATIONSHIP].

        Chapter Title: {{title}}

        Content:
        {{content}}
    """.trimIndent()

    val DEFAULT_SKIP_RISK_PROMPT = """
        Evaluate the "Skip Risk" for Chapter {{chapterIndex}} based on the provided context.

        Context:
        {{context}}

        Task:
        Classify this chapter into EXACTLY ONE of the following categories:
        - Filler
        - Low Value
        - Skip with Caution
        - Must Read

        Definitions:
        - Filler: Irrelevant to the main plot, purely padding.
        - Low Value: Minor details, safe to skip.
        - Skip with Caution: Contains some relevant details; skip at your own risk.
        - Must Read: Critical plot points that genuinely impact subsequent understanding.

        Constraint:
        Output ONLY the category name. Do not output numbers, punctuation, or explanations.
        When in doubt, favor "Skip with Caution" over "Must Read".
    """.trimIndent()

    data class AITask(
        val key: String,
        val bookUrl: String,
        val chapterIndex: Int,
        val feature: String,
        val createdAt: Long
    )

    private data class TaskEntry(
        val task: AITask,
        val job: Job
    )

    fun tasks(): StateFlow<List<AITask>> = tasksFlow.asStateFlow()

    fun getSummaryProviderId(): Long? = appCtx.getPrefString(PreferKey.aiSummaryProviderId)?.toLongOrNull()
    fun getSummaryPromptId(): Long? = appCtx.getPrefString(PreferKey.aiSummaryPromptId)?.toLongOrNull()
    fun getSkipRiskProviderId(): Long? = appCtx.getPrefString(PreferKey.aiSkipRiskProviderId)?.toLongOrNull()
    fun getSkipRiskPromptId(): Long? = appCtx.getPrefString(PreferKey.aiSkipRiskPromptId)?.toLongOrNull()

    fun generateSummary(book: Book, chapter: BookChapter, force: Boolean = false): Job? {

        val jobKey = "${book.bookUrl}-${chapter.index}-$FEATURE_SUMMARY"
        if (!force) {
            val existingInsight = appDb.chapterInsightDao.get(book.bookUrl, chapter.index)
            if (!existingInsight?.summary.isNullOrBlank()) {
                return null
            }
            if (existingInsight?.status == STATUS_FAILED) {
                return null
            }
        }
        val task = AITask(
            key = jobKey,
            bookUrl = book.bookUrl,
            chapterIndex = chapter.index,
            feature = FEATURE_SUMMARY,
            createdAt = System.currentTimeMillis()
        )
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                updateStatus(book.bookUrl, chapter.index, STATUS_GENERATING)

                val content = BookHelp.getContent(book, chapter)
                val normalizedLen = content?.replace(Regex("\\p{P}|\\s+"), "")?.length ?: 0
                var summaryText: String? = null
                if (content.isNullOrBlank()) {
                    summaryText = if (chapter.title.isBlank()) " \n " else chapter.title
                } else if (normalizedLen in 1..499) {
                    summaryText = content
                }

                if (summaryText == null) {
                    val providerId = getSummaryProviderId() ?: throw Exception("Summary Provider not configured")
                    val provider = appDb.aiProviderDao.get(providerId) ?: throw Exception("Summary Provider not available")
                    val promptId = getSummaryPromptId()
                    val promptObj = if (promptId != null) appDb.aiSummaryPromptDao.get(promptId) else null
                    val promptTemplate = if (promptObj == null || promptObj.prompt.isBlank()) DEFAULT_SUMMARY_PROMPT else promptObj.prompt
                    val prompt = promptTemplate
                        .replace("{{title}}", chapter.title)
                        .replace("{{content}}", content ?: "")
                    val messages = listOf(
                        mapOf("role" to "system", "content" to "You are a helpful reading assistant."),
                        mapOf("role" to "user", "content" to prompt)
                    )
                    val result = AIClient.generate(provider, messages)
                    summaryText = result
                }

                val insight = appDb.chapterInsightDao.get(book.bookUrl, chapter.index)
                    ?: ChapterInsight(bookUrl = book.bookUrl, chapterIndex = chapter.index)

                insight.summary = summaryText
                insight.status = STATUS_READY
                insight.timestamp = System.currentTimeMillis()

                appDb.chapterInsightDao.insert(insight)
                postEvent(EventBus.INSIGHT_UPDATED, chapter.index)

            } catch (e: CancellationException) {
                updateStatus(book.bookUrl, chapter.index, STATUS_NONE)
                throw e
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleAIError(e, "Summary Generation Failed") {
                        scope.launch { generateSummary(book, chapter, force = true) }
                    }
                    e.printStackTrace()
                    updateStatus(book.bookUrl, chapter.index, STATUS_FAILED)
                }
            } finally {
                queue.remove(jobKey)
                updateTasksFlow()
            }
        }
        val existing = queue.putIfAbsent(jobKey, TaskEntry(task, job))
        if (existing != null) {
            job.cancel()
            return existing.job
        }
        updateTasksFlow()
        job.start()
        return job
    }

    fun generateSkipRisk(book: Book, chapterIndex: Int, force: Boolean = false) {
         if (!SKIP_RISK_ENABLED) return

         val providerId = getSkipRiskProviderId() ?: return
         val provider = appDb.aiProviderDao.get(providerId) ?: return

         val jobKey = "${book.bookUrl}-$chapterIndex-$FEATURE_SKIP_RISK"
         if (!force) {
             val existingInsight = appDb.chapterInsightDao.get(book.bookUrl, chapterIndex)
             if ((existingInsight?.skipRiskLabel ?: 0) > 0) {
                 return
             }
         }
         val task = AITask(
             key = jobKey,
             bookUrl = book.bookUrl,
             chapterIndex = chapterIndex,
             feature = FEATURE_SKIP_RISK,
             createdAt = System.currentTimeMillis()
         )
         val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                DebugLog.d("InsightManager", "Starting Skip Risk generation for $jobKey")

                // Fetch contexts: N-3..N-1 summaries, N full text, N+1..N+3 summaries
                val indices = ((chapterIndex - 3) .. (chapterIndex + 3)).toList()
                val insights = appDb.chapterInsightDao.getBatch(book.bookUrl, indices).associateBy { it.chapterIndex }

                // Check existence of chapters
                val chapters = appDb.bookChapterDao.getChapterList(book.bookUrl, indices.first(), indices.last())
                val chapterMap = chapters.associateBy { it.index }

                val targetChapter = chapterMap[chapterIndex] ?: return@launch

                val contextBuilder = StringBuilder()

                // Prior Summaries
                for (i in (chapterIndex - 3) until chapterIndex) {
                    val ch = chapterMap[i] ?: continue
                    val summary = ensureSummary(book, ch)
                    if (summary == null) {
                        DebugLog.w("InsightManager", "Failed to get summary for chapter $i after retry, aborting skip risk")
                        return@launch
                    }
                    contextBuilder.append("Chapter $i Summary: $summary\n\n")
                }

                // Current Chapter Text
                val content = BookHelp.getContent(book, targetChapter) ?: ""
                contextBuilder.append("Chapter $chapterIndex Content: $content\n\n")

                // Future Summaries
                for (i in (chapterIndex + 1) .. (chapterIndex + 3)) {
                    val ch = chapterMap[i] ?: continue
                    val summary = ensureSummary(book, ch)
                    if (summary == null) {
                        DebugLog.w("InsightManager", "Failed to get summary for chapter $i after retry, aborting skip risk")
                        return@launch
                    }
                    contextBuilder.append("Chapter $i Summary: $summary\n\n")
                }

                // All ready, generate Skip Risk
                val promptId = getSkipRiskPromptId()
                val promptObj = if (promptId != null) appDb.aiSkipRiskPromptDao.get(promptId) else null
                val promptTemplate = if (promptObj == null || promptObj.prompt.isBlank()) DEFAULT_SKIP_RISK_PROMPT else promptObj.prompt
                val prompt = promptTemplate
                    .replace("{{chapterIndex}}", chapterIndex.toString())
                    .replace("{{context}}", contextBuilder.toString())

                val messages = listOf(
                   mapOf("role" to "system", "content" to "You are a reading assistant. Your sole task is to classify the chapter. Output ONLY the label."),
                   mapOf("role" to "user", "content" to prompt)
               )

               DebugLog.d("InsightManager", "Prompt generated, sending to AIClient")
               val result = AIClient.generate(provider, messages).trim()
               DebugLog.d("InsightManager", "AIClient result: $result")

               var label = 0
               if (result.contains("Filler", true)) label = 1
               else if (result.contains("Low Value", true)) label = 2
               else if (result.contains("Skip with Caution", true)) label = 3
               else if (result.contains("Must Read", true)) label = 4
               // Fallback if the model still outputs numbers (less likely now)
               else if (result.startsWith("1")) label = 1
               else if (result.startsWith("2")) label = 2
               else if (result.startsWith("3")) label = 3
               else if (result.startsWith("4")) label = 4

               DebugLog.d("InsightManager", "Parsed label: $label")

               val insight = appDb.chapterInsightDao.get(book.bookUrl, chapterIndex)
                   ?: ChapterInsight(bookUrl = book.bookUrl, chapterIndex = chapterIndex)

               insight.skipRiskLabel = label

               appDb.chapterInsightDao.insert(insight)
               postEvent(EventBus.INSIGHT_UPDATED, chapterIndex)
               DebugLog.d("InsightManager", "Insight inserted/updated for chapter $chapterIndex with label $label")

            } catch (e: CancellationException) {
                DebugLog.d("InsightManager", "Job cancelled: $jobKey")
                throw e
            } catch (e: Exception) {
                  if (e !is CancellationException) {
                      handleAIError(e, "Skip Risk Labeling Failed") {
                          scope.launch { generateSkipRisk(book, chapterIndex, force = true) }
                      }
                      DebugLog.e("InsightManager", "Error generating skip risk", e)
                  }
                  e.printStackTrace()
              } finally {
                DebugLog.d("InsightManager", "Removing task from queue: $jobKey")
                queue.remove(jobKey)
                updateTasksFlow()
            }
        }
         val existing = queue.putIfAbsent(jobKey, TaskEntry(task, job))
         if (existing != null) {
             job.cancel()
             return
         }
         updateTasksFlow()
         job.start()
    }


    fun generateBatchSkipRisk(book: Book, startIndex: Int, count: Int) {
        if (!SKIP_RISK_ENABLED) return

        scope.launch {
            val totalChapters = appDb.bookChapterDao.getChapterCount(book.bookUrl)
            val end = (startIndex + count).coerceAtMost(totalChapters)
            DebugLog.d("InsightManager", "Batch generating skip risk for ${book.name} from $startIndex to $end")

            for (i in startIndex until end) {
                generateSkipRisk(book, i)
            }
        }
    }

    fun deleteBatchInsights(bookUrl: String, startIndex: Int, count: Int) {
        scope.launch {
            val endIndex = startIndex + count

            // 1. Cancel related tasks
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                // Check if task belongs to the book and range
                val task = entry.value.task
                if (task.bookUrl == bookUrl && task.chapterIndex >= startIndex && task.chapterIndex < endIndex) {
                    entry.value.job.cancel()
                    iterator.remove()
                    DebugLog.d("InsightManager", "Cancelled task for deletion: ${task.key}")
                }
            }
            updateTasksFlow()

            // 2. Delete from DB
            appDb.chapterInsightDao.deleteBatch(bookUrl, startIndex, endIndex)
            DebugLog.d("InsightManager", "Deleted insights for $bookUrl from $startIndex to $endIndex")
        }
    }

    private fun updateStatus(bookUrl: String, chapterIndex: Int, status: Int) {
        val insight = appDb.chapterInsightDao.get(bookUrl, chapterIndex)
            ?: ChapterInsight(bookUrl = bookUrl, chapterIndex = chapterIndex)
        insight.status = status
        appDb.chapterInsightDao.insert(insight)
    }

    fun cancelAll() {
        queue.values.forEach { entry ->
            if (entry.task.feature == FEATURE_SUMMARY) {
                updateStatus(entry.task.bookUrl, entry.task.chapterIndex, STATUS_NONE)
            }
            entry.job.cancel()
        }
        queue.clear()
        updateTasksFlow()
    }

    private fun updateTasksFlow() {
        tasksFlow.value = queue.values
            .map { it.task }
            .sortedBy { it.createdAt }
    }

    private fun handleAIError(e: Exception, title: String, retryAction: (() -> Unit)? = null) {
        if (e is CancellationException) return

        val (type, msg, label) = when (e) {
            is AIException -> {
                when (e.code) {
                    in 400..499 -> Triple(AIErrorType.CLIENT, "Client Error (${e.code}): ${e.message}", "Settings")
                    in 500..599 -> Triple(AIErrorType.SERVER, "Server Error (${e.code}): ${e.message}", "Retry")
                    else -> Triple(AIErrorType.UNKNOWN, "API Error: ${e.message}", null)
                }
            }
            is java.net.SocketTimeoutException, is java.net.UnknownHostException, is java.io.IOException -> {
                Triple(AIErrorType.NETWORK, "Network Error: ${e.localizedMessage}", "Retry")
            }
            else -> Triple(AIErrorType.UNKNOWN, "Error: ${e.localizedMessage}", null)
        }

        val onAction = if (label == "Retry") retryAction else null

        DebugLog.e("InsightManager", "$title: $msg", e)
        postEvent(EventBus.AI_ERROR, AIErrorEvent(title, msg, type, label, onAction))
    }

    private suspend fun ensureSummary(book: Book, chapter: BookChapter): String? {
        val chapterIndex = chapter.index
        val bookUrl = book.bookUrl

        // 1. Check existing
        var insight = appDb.chapterInsightDao.get(bookUrl, chapterIndex)

        if (insight != null) {
             DebugLog.d("InsightManager", "ensureSummary: Found insight for ch:$chapterIndex status=${insight.status} summaryLen=${insight.summary?.length}")
        } else {
             DebugLog.d("InsightManager", "ensureSummary: No insight found for ch:$chapterIndex")
        }

        if (!insight?.summary.isNullOrBlank()) {
            return insight!!.summary
        }

        DebugLog.d("InsightManager", "Waiting for summary of chapter $chapterIndex")

        // 2. Try Generate (Standard)
        var job = generateSummary(book, chapter)
        if (job == null) {
             DebugLog.d("InsightManager", "ensureSummary: generateSummary returned null job for ch:$chapterIndex (maybe already running or failed state)")
        }
        job?.join()

        // 3. Re-check
        insight = appDb.chapterInsightDao.get(bookUrl, chapterIndex)
        if (!insight?.summary.isNullOrBlank()) {
            DebugLog.d("InsightManager", "ensureSummary: Summary generated successfully for ch:$chapterIndex")
            return insight!!.summary
        }

        // 4. Retry Logic (Recovery)
        DebugLog.w("InsightManager", "Summary missing for chapter $chapterIndex. Insight status: ${insight?.status}. Attempting recovery...")

        // Cleanup: Delete the failed record
        if (insight != null) {
             DebugLog.d("InsightManager", "ensureSummary: Deleting failed insight record for ch:$chapterIndex")
             appDb.chapterInsightDao.delete(insight)
        }

        // Force Generate
        job = generateSummary(book, chapter, force = true)
        job?.join()

        // 5. Final Check
        insight = appDb.chapterInsightDao.get(bookUrl, chapterIndex)
        val result = insight?.summary
        DebugLog.d("InsightManager", "ensureSummary: Final result for ch:$chapterIndex is ${if (result.isNullOrBlank()) "MISSING" else "FOUND"}")
        return result
    }
}
