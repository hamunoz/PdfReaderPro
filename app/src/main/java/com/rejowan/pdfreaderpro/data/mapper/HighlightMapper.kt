package com.rejowan.pdfreaderpro.data.mapper

import com.rejowan.pdfreaderpro.data.local.database.entity.AnnotationEntity
import com.rejowan.pdfreaderpro.domain.model.Highlight
import com.rejowan.pdfreaderpro.domain.model.HighlightQuad
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Annotation type discriminator for rows that represent a text highlight. */
const val ANNOTATION_TYPE_HIGHLIGHT = "highlight"

/** Fallback when a row has no colour stored. Matches the viewer's default yellow. */
private const val DEFAULT_HIGHLIGHT_COLOR = 0xFFFFFF98.toInt()

private val json = Json { ignoreUnknownKeys = true }

/**
 * Decodes the stored quad JSON.
 *
 * Returns an empty list for malformed or missing JSON rather than throwing, so one
 * bad row cannot take down the reader. A highlight with no quads simply does not
 * render, but still appears in the panel with its text intact.
 */
fun decodeQuads(raw: String?): List<HighlightQuad> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        json.decodeFromString<List<HighlightQuad>>(raw)
    } catch (e: SerializationException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }
}

fun encodeQuads(quads: List<HighlightQuad>): String = json.encodeToString(quads)

fun AnnotationEntity.toHighlight(): Highlight = Highlight(
    id = id,
    pdfPath = pdfPath,
    pageNumber = pageNumber,
    text = selectedText.orEmpty(),
    quads = decodeQuads(quads),
    color = color ?: DEFAULT_HIGHLIGHT_COLOR,
    label = label,
    note = content,
    sortIndex = sortIndex,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Highlight.toEntity(): AnnotationEntity = AnnotationEntity(
    id = id,
    pdfPath = pdfPath,
    pageNumber = pageNumber,
    type = ANNOTATION_TYPE_HIGHLIGHT,
    content = note,
    color = color,
    selectedText = text,
    quads = encodeQuads(quads),
    label = label,
    sortIndex = sortIndex,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Opaque variant of a highlight colour, for swatches and the panel's colour bar.
 *
 * Bit math rather than [android.graphics.Color] so this stays usable from plain JVM
 * unit tests, which have no Android framework.
 */
fun Int.asOpaqueSwatch(): Int = this or ALPHA_OPAQUE

private const val ALPHA_OPAQUE = 0xFF000000.toInt()
