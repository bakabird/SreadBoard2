package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.AIRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AIRuleDao {
    @Query("SELECT * FROM ai_rules ORDER BY id DESC")
    fun flowAll(): Flow<List<AIRule>>

    @Query("SELECT * FROM ai_rules WHERE id = :id")
    fun get(id: Long): AIRule?

    @Query("SELECT * FROM ai_rules WHERE enabled = 1")
    fun getEnabled(): List<AIRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rule: AIRule)

    @Update
    fun update(vararg rule: AIRule)

    @Delete
    fun delete(vararg rule: AIRule)
}
