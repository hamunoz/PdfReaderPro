package com.rejowan.pdfreaderpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Reader settings that belong to one document rather than the whole app.
 *
 * Kept in its own table rather than hung off `recent`, so clearing the recent
 * files list does not silently wipe a document's settings.
 *
 * Keyed on the file path, the same identity bookmarks and highlights use.
 */
@Entity(tableName = "file_preferences")
data class FilePreferenceEntity(
    @PrimaryKey
    val pdfPath: String,

    /**
     * Freezes horizontal panning at wherever the page currently sits.
     *
     * Only meaningful in vertical scroll mode. Applying it in horizontal mode
     * would block the axis the document scrolls along and strand the reader.
     */
    val lockHorizontalScroll: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis()
)
