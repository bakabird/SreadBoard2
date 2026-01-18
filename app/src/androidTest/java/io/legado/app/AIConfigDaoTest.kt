package io.legado.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.AIProvider
import io.legado.app.data.entities.AISummaryPrompt
import io.legado.app.data.entities.AISkipRiskPrompt
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AIConfigDaoTest {

    @Test
    fun providerAndPromptCrud() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val providerDao = db.aiProviderDao
        val summaryDao = db.aiSummaryPromptDao
        val skipDao = db.aiSkipRiskPromptDao

        val provider = AIProvider(name = "Test Provider", baseUrl = "https://example.com", model = "gpt-test")
        providerDao.insert(provider)
        val loadedProvider = providerDao.get(provider.id)
        assertEquals("Test Provider", loadedProvider?.name)
        assertEquals("https://example.com", loadedProvider?.baseUrl)
        assertEquals("gpt-test", loadedProvider?.model)

        val summaryPrompt = AISummaryPrompt(name = "Summary", prompt = "Prompt {{title}} {{content}}")
        summaryDao.insert(summaryPrompt)
        val loadedSummary = summaryDao.get(summaryPrompt.id)
        assertEquals("Summary", loadedSummary?.name)
        assertEquals("Prompt {{title}} {{content}}", loadedSummary?.prompt)

        val skipPrompt = AISkipRiskPrompt(name = "Skip", prompt = "Prompt {{chapterIndex}} {{context}}")
        skipDao.insert(skipPrompt)
        val loadedSkip = skipDao.get(skipPrompt.id)
        assertEquals("Skip", loadedSkip?.name)
        assertEquals("Prompt {{chapterIndex}} {{context}}", loadedSkip?.prompt)

        db.close()
    }
}

