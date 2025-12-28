package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.ChapterInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterInsightDao {
    @Query("SELECT * FROM chapter_insights WHERE bookUrl = :bookUrl AND chapterIndex = :index")
    fun get(bookUrl: String, index: Int): ChapterInsight?

    @Query("SELECT * FROM chapter_insights WHERE bookUrl = :bookUrl AND chapterIndex = :index")
    fun flow(bookUrl: String, index: Int): Flow<ChapterInsight?>

    @Query("SELECT * FROM chapter_insights WHERE bookUrl = :bookUrl")
    fun flowByBook(bookUrl: String): Flow<List<ChapterInsight>>
    
    @Query("SELECT * FROM chapter_insights WHERE bookUrl = :bookUrl AND chapterIndex IN (:indices)")
    fun getBatch(bookUrl: String, indices: List<Int>): List<ChapterInsight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg insight: ChapterInsight)

    @Update
    fun update(vararg insight: ChapterInsight)

    @Delete
    fun delete(vararg insight: ChapterInsight)
}
