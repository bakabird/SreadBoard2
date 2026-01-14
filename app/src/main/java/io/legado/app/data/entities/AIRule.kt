package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "ai_rules")
data class AIRule(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String = "",
    var baseUrl: String = "",
    var apiKey: String = "",
    var model: String = "",
    var concurrentLimit: Int = 1,
    var enabled: Boolean = true,
    var summaryPrompt: String = "",
    var skipRiskPrompt: String = ""
) : Parcelable
