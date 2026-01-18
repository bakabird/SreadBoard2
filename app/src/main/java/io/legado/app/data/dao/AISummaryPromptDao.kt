package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.AISummaryPrompt
import kotlinx.coroutines.flow.Flow

@Dao
interface AISummaryPromptDao {
    @Query("SELECT * FROM ai_summary_prompts ORDER BY id DESC")
    fun flowAll(): Flow<List<AISummaryPrompt>>

    @Query("SELECT * FROM ai_summary_prompts WHERE id = :id")
    fun get(id: Long): AISummaryPrompt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg prompt: AISummaryPrompt)

    @Update
    fun update(vararg prompt: AISummaryPrompt)

    @Delete
    fun delete(vararg prompt: AISummaryPrompt)
}
