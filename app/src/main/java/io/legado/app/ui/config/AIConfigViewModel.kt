package io.legado.app.ui.config

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.AIProvider
import io.legado.app.data.entities.AISummaryPrompt
import io.legado.app.data.entities.AISkipRiskPrompt
import io.legado.app.utils.putPrefString
import splitties.init.appCtx

class AIConfigViewModel(application: Application) : BaseViewModel(application) {

    val providersFlow: Flow<List<AIProvider>> = appDb.aiProviderDao.flowAll()
    val summaryPromptsFlow: Flow<List<AISummaryPrompt>> = appDb.aiSummaryPromptDao.flowAll()
    val skipRiskPromptsFlow: Flow<List<AISkipRiskPrompt>> = appDb.aiSkipRiskPromptDao.flowAll()

    // Provider
    fun saveProvider(provider: AIProvider) {
        viewModelScope.launch {
            if (provider.id == 0L) {
                appDb.aiProviderDao.insert(provider)
            } else {
                appDb.aiProviderDao.update(provider)
            }
        }
    }

    fun deleteProvider(provider: AIProvider) {
        viewModelScope.launch {
            appDb.aiProviderDao.delete(provider)
        }
    }

    // Summary Prompt
    fun saveSummaryPrompt(prompt: AISummaryPrompt) {
        viewModelScope.launch {
            if (prompt.id == 0L) {
                appDb.aiSummaryPromptDao.insert(prompt)
            } else {
                appDb.aiSummaryPromptDao.update(prompt)
            }
        }
    }

    fun deleteSummaryPrompt(prompt: AISummaryPrompt) {
        viewModelScope.launch {
            appDb.aiSummaryPromptDao.delete(prompt)
        }
    }

    // Skip Risk Prompt
    fun saveSkipRiskPrompt(prompt: AISkipRiskPrompt) {
        viewModelScope.launch {
            if (prompt.id == 0L) {
                appDb.aiSkipRiskPromptDao.insert(prompt)
            } else {
                appDb.aiSkipRiskPromptDao.update(prompt)
            }
        }
    }

    fun deleteSkipRiskPrompt(prompt: AISkipRiskPrompt) {
        viewModelScope.launch {
            appDb.aiSkipRiskPromptDao.delete(prompt)
        }
    }

    // Binding
    fun setSummaryProvider(id: Long) {
        appCtx.putPrefString(PreferKey.aiSummaryProviderId, id.toString())
    }

    fun setSummaryPrompt(id: Long) {
        appCtx.putPrefString(PreferKey.aiSummaryPromptId, id.toString())
    }

    fun setSkipRiskProvider(id: Long) {
        appCtx.putPrefString(PreferKey.aiSkipRiskProviderId, id.toString())
    }

    fun setSkipRiskPrompt(id: Long) {
        appCtx.putPrefString(PreferKey.aiSkipRiskPromptId, id.toString())
    }
}
