package io.legado.app.ui.config

import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.AISkipRiskPrompt
import io.legado.app.databinding.Item1lineTextBinding

class AISkipRiskPromptAdapter(val activity: AIConfigActivity) : RecyclerAdapter<AISkipRiskPrompt, Item1lineTextBinding>(activity) {

    override fun getViewBinding(parent: ViewGroup): Item1lineTextBinding {
        return Item1lineTextBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: Item1lineTextBinding,
        item: AISkipRiskPrompt,
        payloads: MutableList<Any>
    ) {
        binding.textView.text = item.name
    }

    override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                activity.editSkipRiskPrompt(it)
            }
        }
        holder.itemView.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let {
                activity.showSkipRiskPromptMenu(it)
            }
            true
        }
    }
}
