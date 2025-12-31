package io.legado.app.lib.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceViewHolder
import io.legado.app.R

class MultiSelectListPreference(context: Context, attrs: AttributeSet) :
    MultiSelectListPreference(context, attrs) {

    private val isBottomBackground: Boolean
    private var onLongClick: ((preference: MultiSelectListPreference) -> Boolean)? = null

    init {
        layoutResource = R.layout.view_preference
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference)
        isBottomBackground = typedArray.getBoolean(R.styleable.Preference_isBottomBackground, false)
        typedArray.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        Preference.bindView<android.view.View>(
            context, holder, icon, title, summary,
            isBottomBackground = isBottomBackground
        )
        super.onBindViewHolder(holder)
        onLongClick?.let { listener ->
            holder.itemView.setOnLongClickListener {
                listener.invoke(this)
            }
        }
    }

    fun onLongClick(listener: (preference: MultiSelectListPreference) -> Boolean) {
        onLongClick = listener
    }

}
