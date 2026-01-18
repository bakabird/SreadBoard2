package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "ai_skip_risk_prompts")
data class AISkipRiskPrompt(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String = "",
    var prompt: String = ""
) : Parcelable
