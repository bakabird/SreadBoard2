package io.legado.app.ui.config

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.AIRule
import io.legado.app.utils.putPrefString
import splitties.init.appCtx

class AIConfigViewModel(application: Application) : BaseViewModel(application) {

    val rulesFlow: Flow<List<AIRule>> = appDb.aiRuleDao.flowAll()

    fun saveRule(rule: AIRule) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                appDb.aiRuleDao.insert(rule)
            } else {
                appDb.aiRuleDao.update(rule)
            }
        }
    }

    fun deleteRule(rule: AIRule) {
        viewModelScope.launch {
            appDb.aiRuleDao.delete(rule)
        }
    }

    fun setSummaryRule(id: Long) {
        appCtx.putPrefString(PreferKey.aiRuleSummary, id.toString())
    }

    fun setSkipRiskRule(id: Long) {
        appCtx.putPrefString(PreferKey.aiRuleSkipRisk, id.toString())
    }
}
