package io.legado.app.ui.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.ai.InsightManager
import io.legado.app.utils.gone
import io.legado.app.utils.setLayout
import io.legado.app.utils.visible
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AITaskQueueDialog : BaseDialogFragment(R.layout.dialog_recycler_view) {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy { TaskAdapter() }

    override fun onStart() {
        super.onStart()
        setLayout(0.95f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = "AI Task Queue"
        binding.toolBar.menu.add("Abandon all tasks").setOnMenuItemClickListener {
            confirmAbandonAll()
            true
        }

        binding.rotateLoading.gone()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            InsightManager.tasks().collectLatest { tasks ->
                adapter.submitList(tasks)
                if (tasks.isEmpty()) {
                    binding.tvMsg.text = "No AI tasks running"
                    binding.tvMsg.visible()
                } else {
                    binding.tvMsg.gone()
                }
            }
        }
    }

    private fun confirmAbandonAll() {
        alert(title = "Abandon all AI tasks?", message = "This will stop all AI tasks and clear the queue. Generated results will be kept.") {
            negativeButton("Cancel")
            positiveButton("Abandon") {
                InsightManager.cancelAll()
            }
        }
    }

    private class TaskAdapter : RecyclerView.Adapter<TaskViewHolder>() {
        private var items: List<InsightManager.AITask> = emptyList()

        fun submitList(newItems: List<InsightManager.AITask>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val item = items[position]
            val title = when (item.feature) {
                InsightManager.FEATURE_SUMMARY -> "Summary"
                InsightManager.FEATURE_SKIP_RISK -> "Skip Risk"
                else -> item.feature
            }
            holder.text1.text = "$title · Chapter ${item.chapterIndex + 1}"
            holder.text2.text = item.bookUrl
        }

        override fun getItemCount(): Int = items.size
    }

    private class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text1: TextView = view.findViewById(android.R.id.text1)
        val text2: TextView = view.findViewById(android.R.id.text2)
    }
}
