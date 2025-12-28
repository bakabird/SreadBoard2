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
import io.legado.app.utils.visible
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InsightsBottomSheet(private val book: Book, private val chapter: BookChapter) : BottomSheetDialogFragment() {

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
        binding.tvTaskQueue.setOnClickListener {
            AITaskQueueDialog().show(parentFragmentManager, "AITaskQueueDialog")
        }

        initTabs()
        observeData()

        // Trigger generation if needed
        InsightManager.generateSummary(book, chapter)
        InsightManager.generateSkipRisk(book, chapter.index)

        binding.btnRetrySummary.setOnClickListener {
             InsightManager.generateSummary(book, chapter, force = true)
        }

        binding.btnRetrySkipRisk.setOnClickListener {
             InsightManager.generateSkipRisk(book, chapter.index, force = true)
        }

        binding.btnSkipChapter.setOnClickListener {
            // Callback to skip chapter
            (activity as? Callback)?.onSkipChapter(chapter.index)
            dismiss()
        }
    }

    private fun initTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Summary"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Skip Risk"))

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

                // Update Skip Risk UI
                if (insight.skipRiskLabel > 0) {
                    binding.tvSkipRiskLabel.text = when(insight.skipRiskLabel) {
                        1 -> "Filler"
                        2 -> "Low Value"
                        3 -> "Skip with Caution"
                        4 -> "Must Read"
                        else -> "Unknown"
                    }
                    val color = when(insight.skipRiskLabel) {
                        1 -> 0xFF888888.toInt() // Gray
                        2 -> 0xFFFFA500.toInt() // Orange
                        3 -> 0xFFFF4500.toInt() // Red-Orange
                        4 -> 0xFF008000.toInt() // Green
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface Callback {
        fun onSkipChapter(currentIndex: Int)
    }
}
