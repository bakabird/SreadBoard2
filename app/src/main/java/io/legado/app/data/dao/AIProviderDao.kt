package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.AIProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface AIProviderDao {
    @Query("SELECT * FROM ai_providers ORDER BY id DESC")
    fun flowAll(): Flow<List<AIProvider>>

    @Query("SELECT * FROM ai_providers WHERE id = :id")
    fun get(id: Long): AIProvider?

    @Query("SELECT * FROM ai_providers WHERE enabled = 1")
    fun getEnabled(): List<AIProvider>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg provider: AIProvider)

    @Update
    fun update(vararg provider: AIProvider)

    @Delete
    fun delete(vararg provider: AIProvider)
}
