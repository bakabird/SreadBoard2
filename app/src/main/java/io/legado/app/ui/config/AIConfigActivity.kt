package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.AIProvider
import io.legado.app.data.entities.AISkipRiskPrompt
import io.legado.app.data.entities.AISummaryPrompt
import io.legado.app.databinding.ActivityAiConfigBinding
import io.legado.app.model.ai.InsightManager
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
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.sendToClip
import io.legado.app.utils.getClipText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AIConfigActivity : VMBaseActivity<ActivityAiConfigBinding, AIConfigViewModel>() {

    override val binding by viewBinding(ActivityAiConfigBinding::inflate)
    override val viewModel by viewModels<AIConfigViewModel>()

    private val providerAdapter by lazy { AIProviderAdapter(this) }
    private val summaryPromptAdapter by lazy { AISummaryPromptAdapter(this) }
    private val skipRiskPromptAdapter by lazy { AISkipRiskPromptAdapter(this) }

    private var currentTab = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.ai_config, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_task_queue -> {
                AITaskQueueDialog().show(supportFragmentManager, "AITaskQueueDialog")
                return true
            }
            R.id.menu_logs -> {
                AILogDialog().show(supportFragmentManager, "AILogDialog")
                return true
            }
            R.id.menu_import_rule -> {
                // Simplified import for now, or expand to support importing different types
                importConfig()
                return true
            }
            R.id.menu_export_summary_combo -> {
                exportSummaryCombo()
                return true
            }
            R.id.menu_export_skip_risk_combo -> {
                exportSkipRiskCombo()
                return true
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.titleBar.title = "AI Config"

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.setEdgeEffectColor(primaryColor)

        // Tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Providers"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Summary Prompts"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Skip Risk Prompts"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                updateList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Default to Provider
        updateList()

        binding.fabAdd.applyTint(primaryColor)
        binding.fabAdd.setOnClickListener {
            when (currentTab) {
                0 -> editProvider(null)
                1 -> editSummaryPrompt(null)
                2 -> editSkipRiskPrompt(null)
            }
        }

        binding.tvSummaryRule.setOnClickListener {
            showBindingSelector(true)
        }

        binding.tvSkipRiskRule.setOnClickListener {
            showBindingSelector(false)
        }

        binding.swRequestPreview.isChecked = getPrefBoolean(PreferKey.aiInsightRequestPreview, false)
        binding.swRequestPreview.setOnCheckedChangeListener { _, isChecked ->
            putPrefBoolean(PreferKey.aiInsightRequestPreview, isChecked)
        }
    }

    private fun updateList() {
        when (currentTab) {
            0 -> binding.recyclerView.adapter = providerAdapter
            1 -> binding.recyclerView.adapter = summaryPromptAdapter
            2 -> binding.recyclerView.adapter = skipRiskPromptAdapter
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            viewModel.providersFlow.collectLatest {
                providerAdapter.setItems(it)
                updateBindingDisplay()
            }
        }
        lifecycleScope.launch {
            viewModel.summaryPromptsFlow.collectLatest {
                summaryPromptAdapter.setItems(it)
                updateBindingDisplay()
            }
        }
        lifecycleScope.launch {
            viewModel.skipRiskPromptsFlow.collectLatest {
                skipRiskPromptAdapter.setItems(it)
                updateBindingDisplay()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBindingDisplay() {
        val providers = providerAdapter.getItems()
        val summaryPrompts = summaryPromptAdapter.getItems()
        val skipRiskPrompts = skipRiskPromptAdapter.getItems()

        val summaryProviderId = getPrefString(PreferKey.aiSummaryProviderId)?.toLongOrNull()
        val summaryPromptId = getPrefString(PreferKey.aiSummaryPromptId)?.toLongOrNull()

        val summaryProviderName = providers.find { it.id == summaryProviderId }?.name ?: "None"
        val summaryPromptName = summaryPrompts.find { it.id == summaryPromptId }?.name ?: "Default"

        binding.tvSummaryRule.text = "$summaryProviderName + $summaryPromptName"

        val skipRiskProviderId = getPrefString(PreferKey.aiSkipRiskProviderId)?.toLongOrNull()
        val skipRiskPromptId = getPrefString(PreferKey.aiSkipRiskPromptId)?.toLongOrNull()

        val skipRiskProviderName = providers.find { it.id == skipRiskProviderId }?.name ?: "None"
        val skipRiskPromptName = skipRiskPrompts.find { it.id == skipRiskPromptId }?.name ?: "Default"

        binding.tvSkipRiskRule.text = "$skipRiskProviderName + $skipRiskPromptName"
    }

    private fun showBindingSelector(isSummary: Boolean) {
        val providers = providerAdapter.getItems()
        if (providers.isEmpty()) {
            toast("No providers available.")
            return
        }

        val providerNames = providers.map { it.name }
        selector(title = "Select Provider", items = providerNames) { _, i ->
            val provider = providers[i]
            if (isSummary) {
                viewModel.setSummaryProvider(provider.id)

                val prompts = summaryPromptAdapter.getItems()
                val promptNames = mutableListOf("Default").apply {
                    addAll(prompts.map { it.name })
                }

                selector(title = "Select Prompt", items = promptNames) { _, j ->
                    if (j == 0) {
                        viewModel.setSummaryPrompt(-1L)
                    } else {
                        val prompt = prompts[j - 1]
                        viewModel.setSummaryPrompt(prompt.id)
                    }
                    updateBindingDisplay()
                }
            } else {
                viewModel.setSkipRiskProvider(provider.id)

                val prompts = skipRiskPromptAdapter.getItems()
                val promptNames = mutableListOf("Default").apply {
                    addAll(prompts.map { it.name })
                }

                selector(title = "Select Prompt", items = promptNames) { _, j ->
                    if (j == 0) {
                        viewModel.setSkipRiskPrompt(-1L)
                    } else {
                        val prompt = prompts[j - 1]
                        viewModel.setSkipRiskPrompt(prompt.id)
                    }
                    updateBindingDisplay()
                }
            }
            updateBindingDisplay()
        }
    }

    // --- Provider ---

    fun editProvider(provider: AIProvider?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_rule_edit, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.et_name)
        val etBaseUrl = dialogView.findViewById<android.widget.EditText>(R.id.et_base_url)
        val etApiKey = dialogView.findViewById<android.widget.EditText>(R.id.et_api_key)
        val etModel = dialogView.findViewById<android.widget.EditText>(R.id.et_model)

        // Hide prompt fields in provider edit
        dialogView.findViewById<TextView>(R.id.tv_edit_summary_prompt).visibility = android.view.View.GONE
        dialogView.findViewById<TextView>(R.id.tv_edit_skip_risk_prompt).visibility = android.view.View.GONE

        if (provider != null) {
            etName.setText(provider.name)
            etBaseUrl.setText(provider.baseUrl)
            etApiKey.setText(provider.apiKey)
            etModel.setText(provider.model)
        } else {
            etBaseUrl.setText("https://api.openai.com")
            etModel.setText("gpt-3.5-turbo")
        }

        alert(title = if (provider == null) "Add Provider" else "Edit Provider") {
            customView { dialogView }
            yesButton {
                val newProvider = provider?.copy() ?: AIProvider()
                newProvider.name = etName.text.toString()
                newProvider.baseUrl = etBaseUrl.text.toString()
                newProvider.apiKey = etApiKey.text.toString()
                newProvider.model = etModel.text.toString()

                if (newProvider.name.isBlank()) {
                    toast("Name cannot be empty")
                    return@yesButton
                }
                viewModel.saveProvider(newProvider)
            }
            noButton()
        }
    }

    fun showProviderMenu(provider: AIProvider) {
        selector(title = provider.name, items = listOf("Edit", "Export", "Delete")) { _, i ->
            when (i) {
                0 -> editProvider(provider)
                1 -> exportProvider(provider)
                2 -> deleteProvider(provider)
            }
        }
    }

    private fun exportProvider(provider: AIProvider) {
        alert(title = "Export Provider", message = "Do you want to include the API Key?") {
            positiveButton("Include") {
                val json = GSON.toJson(provider)
                sendToClip(json)
                toast("Provider exported (with Key)")
            }
            negativeButton("Exclude") {
                val safe = provider.copy(apiKey = "")
                val json = GSON.toJson(safe)
                sendToClip(json)
                toast("Provider exported (without Key)")
            }
            neutralButton("Cancel")
        }
    }

    private fun deleteProvider(provider: AIProvider) {
        alert(title = "Delete Provider", message = "Are you sure?") {
            yesButton { viewModel.deleteProvider(provider) }
            noButton()
        }
    }

    // --- Summary Prompt ---

    fun editSummaryPrompt(prompt: AISummaryPrompt?) {
        showPromptEditDialog(
            title = if (prompt == null) "Add Summary Prompt" else "Edit Summary Prompt",
            initialContent = prompt?.prompt ?: InsightManager.DEFAULT_SUMMARY_PROMPT,
            defaultContent = InsightManager.DEFAULT_SUMMARY_PROMPT,
            hint = "Name will be asked after save. Placeholders: {{title}}, {{content}}"
        ) { content ->
             // Ask for name
             val nameDialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
             val etName = nameDialogView.findViewById<android.widget.EditText>(R.id.edit_view)
             etName.hint = "Prompt Name"
             if (prompt != null) etName.setText(prompt.name)

             alert(title = "Prompt Name") {
                 customView { nameDialogView }
                 yesButton {
                     val name = etName.text.toString()
                     if (name.isBlank()) {
                         toast("Name required")
                         return@yesButton
                     }
                     val newPrompt = prompt?.copy(name = name, prompt = content) ?: AISummaryPrompt(name = name, prompt = content)
                     viewModel.saveSummaryPrompt(newPrompt)
                 }
             }
        }
    }

    fun showSummaryPromptMenu(prompt: AISummaryPrompt) {
        selector(title = prompt.name, items = listOf("Edit", "Delete")) { _, i ->
            when (i) {
                0 -> editSummaryPrompt(prompt)
                1 -> deleteSummaryPrompt(prompt)
            }
        }
    }

    private fun deleteSummaryPrompt(prompt: AISummaryPrompt) {
        alert(title = "Delete Prompt", message = "Are you sure?") {
            yesButton { viewModel.deleteSummaryPrompt(prompt) }
            noButton()
        }
    }

    // --- Skip Risk Prompt ---

    fun editSkipRiskPrompt(prompt: AISkipRiskPrompt?) {
        showPromptEditDialog(
            title = if (prompt == null) "Add Skip Risk Prompt" else "Edit Skip Risk Prompt",
            initialContent = prompt?.prompt ?: InsightManager.DEFAULT_SKIP_RISK_PROMPT,
            defaultContent = InsightManager.DEFAULT_SKIP_RISK_PROMPT,
            hint = "Name will be asked after save. Placeholders: {{chapterIndex}}, {{context}}"
        ) { content ->
             val nameDialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
             val etName = nameDialogView.findViewById<android.widget.EditText>(R.id.edit_view)
             etName.hint = "Prompt Name"
             if (prompt != null) etName.setText(prompt.name)

             alert(title = "Prompt Name") {
                 customView { nameDialogView }
                 yesButton {
                     val name = etName.text.toString()
                     if (name.isBlank()) {
                         toast("Name required")
                         return@yesButton
                     }
                     val newPrompt = prompt?.copy(name = name, prompt = content) ?: AISkipRiskPrompt(name = name, prompt = content)
                     viewModel.saveSkipRiskPrompt(newPrompt)
                 }
             }
        }
    }

    fun showSkipRiskPromptMenu(prompt: AISkipRiskPrompt) {
        selector(title = prompt.name, items = listOf("Edit", "Delete")) { _, i ->
            when (i) {
                0 -> editSkipRiskPrompt(prompt)
                1 -> deleteSkipRiskPrompt(prompt)
            }
        }
    }

    private fun deleteSkipRiskPrompt(prompt: AISkipRiskPrompt) {
        alert(title = "Delete Prompt", message = "Are you sure?") {
            yesButton { viewModel.deleteSkipRiskPrompt(prompt) }
            noButton()
        }
    }

    // --- Helpers ---

    private fun showPromptEditDialog(
        title: String,
        initialContent: String,
        defaultContent: String,
        hint: String,
        onSave: (String) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editView = dialogView.findViewById<android.widget.EditText>(R.id.edit_view)
        editView.setText(initialContent)
        editView.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        editView.minLines = 10
        editView.maxLines = 20
        editView.gravity = android.view.Gravity.TOP or android.view.Gravity.START

        alert(title = title, message = hint) {
            customView { dialogView }
            yesButton {
                onSave(editView.text.toString())
            }
            neutralButton("Reset to Default") {
                onSave(defaultContent)
            }
            noButton()
        }
    }

    private fun importConfig() {
        val clipText = getClipText()
        if (clipText.isNullOrBlank()) {
            toast("Clipboard is empty")
            return
        }

        try {
            val summaryCombo = GSON.fromJsonObject<SummaryCombo>(clipText).getOrNull()
            val kind = summaryCombo?.kind
            val isSummaryKind = kind == null || kind == "summary_combo"
            if (isSummaryKind && summaryCombo != null && summaryCombo.provider != null && summaryCombo.prompt != null && summaryCombo.provider.baseUrl.isNotEmpty()) {
                importSummaryCombo(summaryCombo)
                return
            }
        } catch (e: Exception) {
        }

        try {
            val skipCombo = GSON.fromJsonObject<SkipRiskCombo>(clipText).getOrNull()
            if (skipCombo != null && skipCombo.kind == "skip_risk_combo" && skipCombo.provider != null && skipCombo.prompt != null && skipCombo.provider.baseUrl.isNotEmpty()) {
                importSkipRiskCombo(skipCombo)
                return
            }
        } catch (e: Exception) {
        }

        try {
            val provider = GSON.fromJsonObject<AIProvider>(clipText).getOrNull()
            if (provider != null && provider.baseUrl.isNotEmpty()) {
                saveImportedProvider(provider)
                return
            }
        } catch (e: Exception) {
        }

        toast("Could not identify valid config from clipboard")
    }

    private fun saveImportedProvider(provider: AIProvider) {
        lifecycleScope.launch {
            val providers = providerAdapter.getItems()
            var newName = provider.name
            var count = 1
            while (providers.any { it.name == newName }) {
                newName = "${provider.name}($count)"
                count++
            }
            provider.name = newName
            provider.id = 0
            viewModel.saveProvider(provider)
            toast("Imported Provider: $newName")
        }
    }

    private fun importSummaryCombo(combo: SummaryCombo) {
        val provider = combo.provider ?: return
        val prompt = combo.prompt ?: return
        lifecycleScope.launch {
            val providers = providerAdapter.getItems()
            var providerName = provider.name.ifBlank { "AI Provider" }
            var count = 1
            while (providers.any { it.name == providerName }) {
                providerName = "${provider.name}($count)"
                count++
            }
            provider.name = providerName
            provider.id = 0
            viewModel.saveProvider(provider)

            val prompts = summaryPromptAdapter.getItems()
            var promptName = prompt.name.ifBlank { "Summary Prompt" }
            var promptCount = 1
            while (prompts.any { it.name == promptName }) {
                promptName = "${prompt.name}($promptCount)"
                promptCount++
            }
            val newPrompt = AISummaryPrompt(name = promptName, prompt = prompt.prompt)
            viewModel.saveSummaryPrompt(newPrompt)

            toast("Imported Summary combo")
        }
    }

    private fun importSkipRiskCombo(combo: SkipRiskCombo) {
        val provider = combo.provider ?: return
        val prompt = combo.prompt ?: return
        lifecycleScope.launch {
            val providers = providerAdapter.getItems()
            var providerName = provider.name.ifBlank { "AI Provider" }
            var count = 1
            while (providers.any { it.name == providerName }) {
                providerName = "${provider.name}($count)"
                count++
            }
            provider.name = providerName
            provider.id = 0
            viewModel.saveProvider(provider)

            val prompts = skipRiskPromptAdapter.getItems()
            var promptName = prompt.name.ifBlank { "Skip Risk Prompt" }
            var promptCount = 1
            while (prompts.any { it.name == promptName }) {
                promptName = "${prompt.name}($promptCount)"
                promptCount++
            }
            val newPrompt = AISkipRiskPrompt(name = promptName, prompt = prompt.prompt)
            viewModel.saveSkipRiskPrompt(newPrompt)

            toast("Imported Skip Risk combo")
        }
    }

    private fun exportSummaryCombo() {
        val providers = providerAdapter.getItems()
        val summaryPrompts = summaryPromptAdapter.getItems()

        val providerId = getPrefString(PreferKey.aiSummaryProviderId)?.toLongOrNull()
        if (providerId == null) {
            toast("No Summary binding set.")
            return
        }
        val provider = providers.find { it.id == providerId }
        if (provider == null) {
            toast("No Summary provider found.")
            return
        }

        val promptId = getPrefString(PreferKey.aiSummaryPromptId)?.toLongOrNull()
        val prompt = if (promptId != null && promptId > 0) {
            summaryPrompts.find { it.id == promptId }
        } else {
            null
        }
        val effectivePrompt = prompt ?: AISummaryPrompt(name = "Default", prompt = InsightManager.DEFAULT_SUMMARY_PROMPT)

        alert(title = "Export Summary Combo", message = "Do you want to include the API Key?") {
            positiveButton("Include") {
                val combo = SummaryCombo(
                    kind = "summary_combo",
                    provider = provider,
                    prompt = effectivePrompt
                )
                val json = GSON.toJson(combo)
                sendToClip(json)
                toast("Summary combo exported (with Key)")
            }
            negativeButton("Exclude") {
                val safeProvider = provider.copy(apiKey = "")
                val combo = SummaryCombo(
                    kind = "summary_combo",
                    provider = safeProvider,
                    prompt = effectivePrompt
                )
                val json = GSON.toJson(combo)
                sendToClip(json)
                toast("Summary combo exported (without Key)")
            }
            neutralButton("Cancel")
        }
    }

    private fun exportSkipRiskCombo() {
        val providers = providerAdapter.getItems()
        val skipRiskPrompts = skipRiskPromptAdapter.getItems()

        val providerId = getPrefString(PreferKey.aiSkipRiskProviderId)?.toLongOrNull()
        if (providerId == null) {
            toast("No Skip Risk binding set.")
            return
        }
        val provider = providers.find { it.id == providerId }
        if (provider == null) {
            toast("No Skip Risk provider found.")
            return
        }

        val promptId = getPrefString(PreferKey.aiSkipRiskPromptId)?.toLongOrNull()
        val prompt = if (promptId != null && promptId > 0) {
            skipRiskPrompts.find { it.id == promptId }
        } else {
            null
        }
        val effectivePrompt = prompt ?: AISkipRiskPrompt(name = "Default", prompt = InsightManager.DEFAULT_SKIP_RISK_PROMPT)

        alert(title = "Export Skip Risk Combo", message = "Do you want to include the API Key?") {
            positiveButton("Include") {
                val combo = SkipRiskCombo(
                    kind = "skip_risk_combo",
                    provider = provider,
                    prompt = effectivePrompt
                )
                val json = GSON.toJson(combo)
                sendToClip(json)
                toast("Skip Risk combo exported (with Key)")
            }
            negativeButton("Exclude") {
                val safeProvider = provider.copy(apiKey = "")
                val combo = SkipRiskCombo(
                    kind = "skip_risk_combo",
                    provider = safeProvider,
                    prompt = effectivePrompt
                )
                val json = GSON.toJson(combo)
                sendToClip(json)
                toast("Skip Risk combo exported (without Key)")
            }
            neutralButton("Cancel")
        }
    }

    private fun toast(msg: String) {
        toastOnUi(msg)
    }

    private data class SummaryCombo(
        val kind: String? = null,
        val provider: AIProvider? = null,
        val prompt: AISummaryPrompt? = null
    )

    private data class SkipRiskCombo(
        val kind: String? = null,
        val provider: AIProvider? = null,
        val prompt: AISkipRiskPrompt? = null
    )
}
