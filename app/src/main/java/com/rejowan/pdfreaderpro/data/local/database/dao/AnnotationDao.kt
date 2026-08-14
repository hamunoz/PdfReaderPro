package com.rejowan.pdfreaderpro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rejowan.pdfreaderpro.data.local.database.entity.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {

    @Query("SELECT * FROM annotations WHERE pdfPath = :pdfPath ORDER BY pageNumber ASC, createdAt ASC")
    fun getAnnotationsForPdf(pdfPath: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE pdfPath = :pdfPath AND pageNumber = :pageNumber")
    fun getAnnotationsForPage(pdfPath: String, pageNumber: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE id = :id")
    suspend fun getById(id: Long): AnnotationEntity?

    /**
     * Highlights for a PDF, in reading order. The panel and next/previous navigation
     * both use this ordering so they can never disagree about which highlight is next.
     */
    @Query(
        """
        SELECT * FROM annotations
        WHERE pdfPath = :pdfPath AND type = 'highlight'
        ORDER BY pageNumber ASC, sortIndex ASC, createdAt ASC
        """
    )
    fun getHighlightsForPdf(pdfPath: String): Flow<List<AnnotationEntity>>

    /** Highlights whose text contains [query], in the same reading order. */
    @Query(
        """
        SELECT * FROM annotations
        WHERE pdfPath = :pdfPath AND type = 'highlight'
          AND selectedText LIKE '%' || :query || '%'
        ORDER BY pageNumber ASC, sortIndex ASC, createdAt ASC
        """
    )
    fun searchHighlights(pdfPath: String, query: String): Flow<List<AnnotationEntity>>

    /** Highest sortIndex on a page, so a new highlight can be appended after it. */
    @Query(
        """
        SELECT MAX(sortIndex) FROM annotations
        WHERE pdfPath = :pdfPath AND pageNumber = :pageNumber AND type = 'highlight'
        """
    )
    suspend fun getMaxSortIndexForPage(pdfPath: String, pageNumber: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: AnnotationEntity): Long

    @Update
    suspend fun update(annotation: AnnotationEntity)

    @Delete
    suspend fun delete(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM annotations WHERE pdfPath = :pdfPath")
    suspend fun deleteAllForPdf(pdfPath: String)

    @Query("DELETE FROM annotations WHERE pdfPath = :pdfPath AND pageNumber = :pageNumber")
    suspend fun deleteAllForPage(pdfPath: String, pageNumber: Int)

    @Query("DELETE FROM annotations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM annotations WHERE pdfPath = :pdfPath")
    suspend fun getCountForPdf(pdfPath: String): Int
}
