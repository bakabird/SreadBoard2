package io.legado.app.ui.config

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemAppLogBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.ai.AILogItem
import io.legado.app.model.ai.AILogManager
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.LogUtils
import io.legado.app.utils.setLayout
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick
import java.util.*

class AILogDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy {
        LogAdapter(requireContext())
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            toolBar.setBackgroundColor(primaryColor)
            toolBar.setTitle("AI Logs")
            toolBar.inflateMenu(R.menu.app_log)
            toolBar.setOnMenuItemClickListener(this@AILogDialog)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }
        adapter.setItems(AILogManager.logs.toList())
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_clear -> {
                AILogManager.clear()
                adapter.clearItems()
            }
        }
        return true
    }

    inner class LogAdapter(context: Context) :
        RecyclerAdapter<AILogItem, ItemAppLogBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemAppLogBinding {
            return ItemAppLogBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemAppLogBinding,
            item: AILogItem,
            payloads: MutableList<Any>
        ) {
            binding.textTime.text = LogUtils.logTimeFormat.format(Date(item.time))
            val status = item.error ?: "HTTP ${item.responseCode}"
            binding.textMessage.text = "${item.method} ${item.url}\n$status"
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemAppLogBinding) {
            binding.root.onClick {
                getItem(holder.layoutPosition)?.let { item ->
                    val detail = buildString {
                        append("URL: ").append(item.url).append("\n")
                        append("Method: ").append(item.method).append("\n")
                        append("Time: ").append(LogUtils.logTimeFormat.format(Date(item.time))).append("\n")
                        append("Status: ").append(item.error ?: item.responseCode).append("\n\n")
                        append("--- Request ---\n")
                        append("Headers: ").append(item.headers).append("\n")
                        append("Body:\n").append(item.requestBody).append("\n\n")
                        append("--- Response ---\n")
                        append("Body:\n").append(item.responseBody ?: "(empty)")
                    }
                    showDialogFragment(TextDialog("Log Detail", detail))
                }
            }
        }
    }
}
