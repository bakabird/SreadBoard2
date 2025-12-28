package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.AIRule
import io.legado.app.databinding.ActivityAiConfigBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyTint
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AIConfigActivity : VMBaseActivity<ActivityAiConfigBinding, AIConfigViewModel>() {

    override val binding by viewBinding(ActivityAiConfigBinding::inflate)
    override val viewModel by viewModels<AIConfigViewModel>()

    private val adapter by lazy { AIRuleAdapter(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.titleBar.title = "Chapter Insights Config"
        binding.titleBar.menu.add("AI Task Queue").setOnMenuItemClickListener {
            AITaskQueueDialog().show(supportFragmentManager, "AITaskQueueDialog")
            true
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.setEdgeEffectColor(primaryColor)

        binding.fabAdd.applyTint(primaryColor)
        binding.fabAdd.setOnClickListener {
            showEditDialog(null)
        }

        binding.tvSummaryRule.setOnClickListener {
            showRuleSelector(true)
        }

        binding.tvSkipRiskRule.setOnClickListener {
            showRuleSelector(false)
        }

        binding.swRequestPreview.isChecked = getPrefBoolean(PreferKey.aiInsightRequestPreview, false)
        binding.swRequestPreview.setOnCheckedChangeListener { _, isChecked ->
            putPrefBoolean(PreferKey.aiInsightRequestPreview, isChecked)
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            viewModel.rulesFlow.collectLatest { rules ->
                adapter.setItems(rules)
                updateRuleDisplays(rules)
            }
        }
    }

    private fun updateRuleDisplays(rules: List<AIRule>) {
        val summaryRuleId = getPrefString(PreferKey.aiRuleSummary)?.toLongOrNull()
        val skipRiskRuleId = getPrefString(PreferKey.aiRuleSkipRisk)?.toLongOrNull()

        binding.tvSummaryRule.text = rules.find { it.id == summaryRuleId }?.name ?: "Select Rule"
        binding.tvSkipRiskRule.text = rules.find { it.id == skipRiskRuleId }?.name ?: "Select Rule"
    }

    private fun showRuleSelector(isSummary: Boolean) {
        val rules = adapter.getItems()
        if (rules.isEmpty()) {
            toast("No rules available. Please add one first.")
            return
        }

        val names = rules.map { it.name }
        selector(
            title = if (isSummary) "Select Summary Rule" else "Select Skip Risk Rule",
            items = names
        ) { _, index ->
            val rule = rules[index]
            if (isSummary) {
                viewModel.setSummaryRule(rule.id)
            } else {
                viewModel.setSkipRiskRule(rule.id)
            }
            // UI update will happen via flow collection if needed, but simple text update here is fine too
            // Actually flow collection only triggers on db changes, prefer key changes need manual update or another observer
            // For simplicity, we just update text views in initData's flow which triggers when DB changes.
            // Wait, DB doesn't change when pref changes.
            // Let's just update UI manually here or add a pref observer.
            // Manual update for now.
             if (isSummary) {
                binding.tvSummaryRule.text = rule.name
            } else {
                binding.tvSkipRiskRule.text = rule.name
            }
        }
    }

    @SuppressLint("InflateParams")
    fun showEditDialog(rule: AIRule?) {
        // Simple edit dialog for now. A full activity might be better if more fields needed.
        // But plan said Activity/Screens, so let's stick to a simple dialog for MVP or a full custom dialog.
        // Let's use a custom alert dialog with custom view for multiple fields.

        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_rule_edit, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.et_name)
        val etBaseUrl = dialogView.findViewById<android.widget.EditText>(R.id.et_base_url)
        val etApiKey = dialogView.findViewById<android.widget.EditText>(R.id.et_api_key)
        val etModel = dialogView.findViewById<android.widget.EditText>(R.id.et_model)

        if (rule != null) {
            etName.setText(rule.name)
            etBaseUrl.setText(rule.baseUrl)
            etApiKey.setText(rule.apiKey)
            etModel.setText(rule.model)
        } else {
            // Defaults
            etBaseUrl.setText("https://api.openai.com")
            etModel.setText("gpt-3.5-turbo")
        }

        alert(title = if (rule == null) "Add AI Rule" else "Edit AI Rule") {
            customView { dialogView }
            yesButton {
                val newRule = rule?.copy() ?: AIRule()
                newRule.name = etName.text.toString()
                newRule.baseUrl = etBaseUrl.text.toString()
                newRule.apiKey = etApiKey.text.toString()
                newRule.model = etModel.text.toString()

                if (newRule.name.isBlank()) {
                    toast("Name cannot be empty")
                    return@yesButton
                }

                viewModel.saveRule(newRule)
            }
            noButton()
        }
    }

    fun deleteRule(rule: AIRule) {
        alert(title = "Delete Rule", message = "Are you sure you want to delete ${rule.name}?") {
            yesButton {
                viewModel.deleteRule(rule)
            }
            noButton()
        }
    }

    private fun toast(msg: String) {
        toastOnUi(msg)
    }
}
