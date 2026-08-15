package com.rejowan.pdfreaderpro.data.mapper

import com.rejowan.pdfreaderpro.data.local.database.entity.AnnotationEntity
import com.rejowan.pdfreaderpro.domain.model.Highlight
import com.rejowan.pdfreaderpro.domain.model.HighlightSource
import com.rejowan.pdfreaderpro.domain.model.HighlightQuad
import com.rejowan.pdfreaderpro.presentation.components.pdf.model.DocumentHighlight
import com.rejowan.pdfreaderpro.presentation.components.pdf.model.PdfQuad
import com.rejowan.pdfreaderpro.presentation.components.pdf.model.RenderedHighlight
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
 * Converts a highlight the PDF file carries into the shared domain model.
 *
 * Marked [HighlightSource.DOCUMENT], so the panel lists it without edit controls and
 * nothing tries to write it back. Pages arrive 1-based from the viewer and are held
 * 0-based, matching the app's own.
 */
fun DocumentHighlight.toHighlight(pdfPath: String): Highlight = Highlight(
    id = id,
    pdfPath = pdfPath,
    pageNumber = pageNumber - 1,
    text = text,
    quads = quads.map { HighlightQuad(it.x, it.y, it.w, it.h) },
    color = if (color == -1) DEFAULT_HIGHLIGHT_COLOR else color,
    label = label.takeIf { it.isNotBlank() },
    note = note.takeIf { it.isNotBlank() },
    source = HighlightSource.DOCUMENT
)

/**
 * Converts a stored highlight into the form the viewer draws from.
 *
 * The page number is the boundary that matters here: highlights are stored 0-based
 * but the viewer indexes pages 1-based, and getting it wrong draws every highlight
 * one page early, which nothing in the data layer would reveal.
 *
 * @param alpha Opacity for the fill, since the overlay paints over the page.
 */
fun Highlight.toRendered(alpha: Float): RenderedHighlight = RenderedHighlight(
    id = id,
    pageNumber = pageNumber + 1,
    color = color.toCssRgba(alpha),
    quads = quads.map { PdfQuad(it.x, it.y, it.w, it.h) }
)

/** CSS colour string for an ARGB int, with the alpha replaced. */
fun Int.toCssRgba(alpha: Float): String {
    val red = (this shr 16) and 0xFF
    val green = (this shr 8) and 0xFF
    val blue = this and 0xFF
    return "rgba($red, $green, $blue, $alpha)"
}

/**
 * Opaque variant of a highlight colour, for swatches and the panel's colour bar.
 *
 * Bit math rather than [android.graphics.Color] so this stays usable from plain JVM
 * unit tests, which have no Android framework.
 */
fun Int.asOpaqueSwatch(): Int = this or ALPHA_OPAQUE

private const val ALPHA_OPAQUE = 0xFF000000.toInt()
