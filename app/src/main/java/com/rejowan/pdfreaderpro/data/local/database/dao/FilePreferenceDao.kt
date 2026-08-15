package com.rejowan.pdfreaderpro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rejowan.pdfreaderpro.data.local.database.entity.FilePreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilePreferenceDao {

    /** Emits null until the document has settings of its own. */
    @Query("SELECT * FROM file_preferences WHERE pdfPath = :pdfPath")
    fun observe(pdfPath: String): Flow<FilePreferenceEntity?>

    @Query("SELECT * FROM file_preferences WHERE pdfPath = :pdfPath")
    suspend fun get(pdfPath: String): FilePreferenceEntity?

    /** Upsert, since a document has no row until it needs one. */
    @Upsert
    suspend fun save(preference: FilePreferenceEntity)

    @Query("DELETE FROM file_preferences WHERE pdfPath = :pdfPath")
    suspend fun deleteFor(pdfPath: String)

    @Query("DELETE FROM file_preferences")
    suspend fun clearAll()
}
