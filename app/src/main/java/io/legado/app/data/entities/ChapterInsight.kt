package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "chapter_insights",
    primaryKeys = ["bookUrl", "chapterIndex"],
    indices = [Index(value = ["bookUrl"])]
)
data class ChapterInsight(
    var bookUrl: String = "",
    var chapterIndex: Int = 0,
    var summary: String? = null,
    // 0: Unknown, 1: Filler, 2: Low Value, 3: Skip with Caution, 4: Must Read
    var skipRiskLabel: Int = 0,
    // 0: None, 1: Generating, 2: Ready, 3: Failed
    var status: Int = 0,
    var timestamp: Long = System.currentTimeMillis()
) : Parcelable
