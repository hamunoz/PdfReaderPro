package com.rejowan.pdfreaderpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for PDF annotations (highlights, notes, etc.)
 * New in v2 - allows users to add annotations to PDFs.
 */
@Entity(
    tableName = "annotations",
    indices = [
        Index(value = ["pdfPath"]),
        Index(value = ["pdfPath", "pageNumber"])
    ]
)
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pdfPath: String,
    val pageNumber: Int,
    val type: String, // "highlight", "underline", "note"
    val content: String?, // For notes, the text content
    val color: Int?, // Highlight/underline color as ARGB int

    /** The highlighted text itself, shown in the highlights panel and searched over. */
    val selectedText: String? = null,

    /**
     * The highlighted region, as a JSON array of rectangles:
     * `[{"x":0.1,"y":0.2,"w":0.3,"h":0.02}, ...]`
     *
     * A selection spanning more than one line needs one rectangle per line, which is
     * why this replaces the single start/end pair below. Coordinates are normalised to
     * 0..1 against the unrotated page box so they survive zoom and rotation without
     * recalculation.
     */
    val quads: String? = null,

    /** Optional user-assigned tag, e.g. "Important" or "Review later". */
    val label: String? = null,

    /** Order within a page. Drives both the panel ordering and next/previous. */
    val sortIndex: Int = 0,

    // Superseded by [quads]. Retained so the v7 -> v8 migration stays additive.
    val startX: Float? = null,
    val startY: Float? = null,
    val endX: Float? = null,
    val endY: Float? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
