package io.legado.app.ui.config

import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.AIRule
import io.legado.app.databinding.Item1lineTextBinding

class AIRuleAdapter(val activity: AIConfigActivity) : RecyclerAdapter<AIRule, Item1lineTextBinding>(activity) {

    override fun getViewBinding(parent: ViewGroup): Item1lineTextBinding {
        return Item1lineTextBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: Item1lineTextBinding,
        item: AIRule,
        payloads: MutableList<Any>
    ) {
        binding.textView.text = item.name
    }

    override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                activity.showEditDialog(it)
            }
        }
        holder.itemView.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let {
                activity.deleteRule(it)
            }
            true
        }
    }
}
