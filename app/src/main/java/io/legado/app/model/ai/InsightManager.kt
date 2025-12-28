package io.legado.app.model.ai

import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ChapterInsight
import io.legado.app.help.book.BookHelp
import io.legado.app.utils.getPrefString
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

    // Status constants matching ChapterInsight
    const val STATUS_NONE = 0
    const val STATUS_GENERATING = 1
    const val STATUS_READY = 2
    const val STATUS_FAILED = 3

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

    fun getSummaryRuleId(): Long? = appCtx.getPrefString(PreferKey.aiRuleSummary)?.toLongOrNull()
    fun getSkipRiskRuleId(): Long? = appCtx.getPrefString(PreferKey.aiRuleSkipRisk)?.toLongOrNull()

    fun generateSummary(book: Book, chapter: BookChapter, force: Boolean = false): Job? {
        val ruleId = getSummaryRuleId() ?: return null
        val rule = appDb.aiRuleDao.get(ruleId) ?: return null

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

                // Get Content
                val content = BookHelp.getContent(book, chapter)
                if (content.isNullOrBlank()) {
                     throw Exception("Content empty")
                }

                // Construct Prompt
                val prompt = """
                    You are a helpful reading assistant.
                    Summarize the following chapter in about 200 words.
                    Provide 5-10 bullet points for key events.
                    If the chapter introduces new concepts or new character relationships, mark them with [NEW CONCEPT] or [NEW RELATIONSHIP].

                    Chapter Title: ${chapter.title}

                    Content:
                    $content
                """.trimIndent()

                val messages = listOf(
                    mapOf("role" to "system", "content" to "You are a helpful reading assistant."),
                    mapOf("role" to "user", "content" to prompt)
                )

                val result = AIClient.generate(rule, messages)

                val insight = appDb.chapterInsightDao.get(book.bookUrl, chapter.index)
                    ?: ChapterInsight(bookUrl = book.bookUrl, chapterIndex = chapter.index)

                insight.summary = result
                insight.status = STATUS_READY
                insight.timestamp = System.currentTimeMillis()

                appDb.chapterInsightDao.insert(insight)

            } catch (e: CancellationException) {
                updateStatus(book.bookUrl, chapter.index, STATUS_NONE)
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                updateStatus(book.bookUrl, chapter.index, STATUS_FAILED)
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
         val ruleId = getSkipRiskRuleId() ?: return
         val rule = appDb.aiRuleDao.get(ruleId) ?: return

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
                     if (chapterMap.containsKey(i)) {
                         var summary = insights[i]?.summary
                         if (summary == null) {
                             val ch = chapterMap[i]!!
                             val summaryJob = generateSummary(book, ch)
                             summaryJob?.join()
                             summary = appDb.chapterInsightDao.get(book.bookUrl, i)?.summary
                             if (summary == null) {
                                 // Summary generation failed or was skipped
                                 return@launch
                             }
                         }
                         contextBuilder.append("Chapter $i Summary: $summary\n\n")
                     }
                 }

                 // Current Chapter Text
                 val content = BookHelp.getContent(book, targetChapter) ?: ""
                 contextBuilder.append("Chapter $chapterIndex Content: $content\n\n")

                 // Future Summaries
                 for (i in (chapterIndex + 1) .. (chapterIndex + 3)) {
                     if (chapterMap.containsKey(i)) {
                         var summary = insights[i]?.summary
                         if (summary == null) {
                             val ch = chapterMap[i]!!
                             val summaryJob = generateSummary(book, ch)
                             summaryJob?.join()
                             summary = appDb.chapterInsightDao.get(book.bookUrl, i)?.summary
                             if (summary == null) {
                                 return@launch
                             }
                         }
                         contextBuilder.append("Chapter $i Summary: $summary\n\n")
                     }
                 }

                 // All ready, generate Skip Risk
                 val prompt = """
                     Evaluate the Skip Risk for Chapter $chapterIndex.

                     Context:
                     $contextBuilder

                     Task:
                     Decide whether to skip this chapter. Return EXACTLY ONE label from:
                     1. Filler
                     2. Low Value
                     3. Skip with Caution
                     4. Must Read

                     Consider:
                     - Advances main plot?
                     - Introduces concepts/relationships?
                     - Sets up later chapters?
                     - Information density?
                 """.trimIndent()

                 val messages = listOf(
                    mapOf("role" to "system", "content" to "You are a helpful reading assistant. Output only one label."),
                    mapOf("role" to "user", "content" to prompt)
                )

                val result = AIClient.generate(rule, messages).trim()

                var label = 0
                if (result.contains("Filler", true)) label = 1
                else if (result.contains("Low Value", true)) label = 2
                else if (result.contains("Skip with Caution", true)) label = 3
                else if (result.contains("Must Read", true)) label = 4

                val insight = appDb.chapterInsightDao.get(book.bookUrl, chapterIndex)
                    ?: ChapterInsight(bookUrl = book.bookUrl, chapterIndex = chapterIndex)

                insight.skipRiskLabel = label

                appDb.chapterInsightDao.insert(insight)

             } catch (e: CancellationException) {
                 throw e
             } catch (e: Exception) {
                 e.printStackTrace()
             } finally {
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
}
