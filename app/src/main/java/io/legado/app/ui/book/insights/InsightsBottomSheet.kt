package io.legado.app.ui.book.insights

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.DialogChapterInsightsBinding
import io.legado.app.model.ai.InsightManager
import io.legado.app.ui.config.AITaskQueueDialog
import io.legado.app.utils.gone
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.visible
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InsightsBottomSheet(
    private val book: Book,
    private val chapter: BookChapter,
    private val readOnly: Boolean = false,
) : BottomSheetDialogFragment() {

    private var _binding: DialogChapterInsightsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogChapterInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = "Chapter ${chapter.index + 1} Insights"
        binding.ivClose.setOnClickListener { dismiss() }
        if (readOnly) {
            binding.ivMore.gone()
            binding.btnRetrySummary.gone()
            binding.btnRetrySkipRisk.gone()
            binding.btnSkipChapter.gone()
        } else {
            binding.ivMore.setOnClickListener {
                showMenu(it)
            }
        }

        initTabs()
        observeData()

        if (readOnly) {
            return
        }

        InsightManager.generateSummary(book, chapter)

        binding.btnRetrySummary.setOnClickListener {
            InsightManager.generateSummary(book, chapter, force = true)
        }

        if (!InsightManager.SKIP_RISK_ENABLED) {
            binding.btnRetrySkipRisk.gone()
        } else {
            InsightManager.generateSkipRisk(book, chapter.index)
            binding.btnRetrySkipRisk.setOnClickListener {
                InsightManager.generateSkipRisk(book, chapter.index, force = true)
            }
        }

        binding.btnSkipChapter.setOnClickListener {
            (activity as? Callback)?.onSkipChapter(chapter.index)
            dismiss()
        }
    }

    private fun initTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Summary"))
        if (InsightManager.SKIP_RISK_ENABLED) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Skip Risk"))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.svSummary.visible()
                        binding.llSkipRisk.gone()
                    }
                    1 -> {
                        binding.svSummary.gone()
                        binding.llSkipRisk.visible()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            appDb.chapterInsightDao.flow(book.bookUrl, chapter.index).collectLatest { insight ->
                if (insight == null) {
                    binding.tvSummaryStatus.text = "Initializing..."
                    binding.tvSummaryStatus.visible()
                    binding.tvSkipRiskStatus.text = "Initializing..."
                    return@collectLatest
                }

                // Update Summary UI
                if (insight.summary != null) {
                    binding.tvSummaryContent.text = insight.summary
                    binding.tvSummaryStatus.gone()
                    binding.btnRetrySummary.gone()
                } else {
                    if (insight.status == InsightManager.STATUS_FAILED) {
                        binding.tvSummaryStatus.text = "Generation Failed"
                        binding.tvSummaryStatus.visible()
                        binding.btnRetrySummary.visible()
                    } else {
                        binding.tvSummaryStatus.text = "Generating Summary..."
                        binding.tvSummaryStatus.visible()
                        binding.btnRetrySummary.gone()
                    }
                }

                if (!InsightManager.SKIP_RISK_ENABLED) {
                    binding.tvSkipRiskLabel.text = "Skip Risk Disabled"
                    binding.tvSkipRiskLabel.backgroundTintList = ColorStateList.valueOf(0xFF888888.toInt())
                    binding.tvSkipRiskStatus.text = "Skip Risk feature is temporarily turned off."
                    binding.tvSkipRiskStatus.visible()
                    binding.btnRetrySkipRisk.gone()
                } else {
                    if (insight.skipRiskLabel > 0) {
                        binding.tvSkipRiskLabel.text = when (insight.skipRiskLabel) {
                            1 -> "Water Chapter"
                            2 -> "Low Value"
                            3 -> "Caution Jump"
                            4 -> "Must Read"
                            else -> "Unknown"
                        }
                        val color = when (insight.skipRiskLabel) {
                            1 -> 0xFF888888.toInt()
                            2 -> 0xFFFFA500.toInt()
                            3 -> 0xFFFF4500.toInt()
                            4 -> 0xFF008000.toInt()
                            else -> 0xFF888888.toInt()
                        }
                        binding.tvSkipRiskLabel.backgroundTintList = ColorStateList.valueOf(color)
                        binding.tvSkipRiskStatus.gone()
                        binding.btnRetrySkipRisk.gone()
                    } else {
                        binding.tvSkipRiskLabel.text = "Analyzing..."
                        binding.tvSkipRiskLabel.backgroundTintList = ColorStateList.valueOf(0xFF888888.toInt())
                        binding.tvSkipRiskStatus.text = "Waiting for context..."
                        binding.tvSkipRiskStatus.visible()
                    }
                }
            }
        }
    }

    private fun showMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "AI Task Queue")
        if (InsightManager.SKIP_RISK_ENABLED) {
            popup.menu.add(0, 2, 0, "Batch Analyze")
        }
        popup.menu.add(0, 3, 0, "Batch Delete")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> AITaskQueueDialog().show(parentFragmentManager, "AITaskQueueDialog")
                2 -> showBatchAnalyzeDialog()
                3 -> showBatchDeleteInput()
            }
            true
        }
        popup.show()
    }

    private fun showBatchDeleteInput() {
        val frameLayout = android.widget.FrameLayout(requireContext())
        val editText = android.widget.EditText(requireContext())
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        editText.hint = "Delete insights for next X chapters"

        val margin = (24 * resources.displayMetrics.density).toInt()
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = margin
        params.rightMargin = margin
        editText.layoutParams = params
        frameLayout.addView(editText)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Batch Delete")
            .setView(frameLayout)
            .setPositiveButton("Confirm") { _, _ ->
                val count = editText.text.toString().toIntOrNull()
                if (count != null && count > 0) {
                    showBatchDeleteConfirmation(count)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBatchDeleteConfirmation(count: Int) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete AI insights for $count chapters starting from the current one? This will also cancel any pending tasks for these chapters.")
            .setPositiveButton("Delete") { _, _ ->
                InsightManager.deleteBatchInsights(book.bookUrl, chapter.index, count)
                toastOnUi("Deleted insights for $count chapters")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBatchAnalyzeDialog() {
        val options = arrayOf("Next 10 Chapters", "Next 20 Chapters", "Next 50 Chapters", "Custom...")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Batch Analyze")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> InsightManager.generateBatchSkipRisk(book, chapter.index + 1, 10)
                    1 -> InsightManager.generateBatchSkipRisk(book, chapter.index + 1, 20)
                    2 -> InsightManager.generateBatchSkipRisk(book, chapter.index + 1, 50)
                    3 -> showCustomBatchDialog()
                }
            }
            .show()
    }

    private fun showCustomBatchDialog() {
        val frameLayout = android.widget.FrameLayout(requireContext())
        val editText = android.widget.EditText(requireContext())
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        editText.hint = "Enter number of chapters"

        val margin = (24 * resources.displayMetrics.density).toInt()
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = margin
        params.rightMargin = margin
        editText.layoutParams = params
        frameLayout.addView(editText)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Custom Batch")
            .setView(frameLayout)
            .setPositiveButton("Confirm") { _, _ ->
                val count = editText.text.toString().toIntOrNull()
                if (count != null && count > 0) {
                    InsightManager.generateBatchSkipRisk(book, chapter.index + 1, count)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface Callback {
        fun onSkipChapter(currentIndex: Int)
    }
}
