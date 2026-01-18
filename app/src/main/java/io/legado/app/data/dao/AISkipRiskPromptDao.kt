package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.AISkipRiskPrompt
import kotlinx.coroutines.flow.Flow

@Dao
interface AISkipRiskPromptDao {
    @Query("SELECT * FROM ai_skip_risk_prompts ORDER BY id DESC")
    fun flowAll(): Flow<List<AISkipRiskPrompt>>

    @Query("SELECT * FROM ai_skip_risk_prompts WHERE id = :id")
    fun get(id: Long): AISkipRiskPrompt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg prompt: AISkipRiskPrompt)

    @Update
    fun update(vararg prompt: AISkipRiskPrompt)

    @Delete
    fun delete(vararg prompt: AISkipRiskPrompt)
}
