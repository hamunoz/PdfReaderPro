package com.rejowan.pdfreaderpro.presentation.components.pdf.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One rectangle of a selected or highlighted region, normalised to 0..1 against the
 * unrotated page box with a top-left origin.
 *
 * The viewer converts through PDF user space rather than screen pixels, so these
 * stay valid across zoom levels and page rotation.
 */
@Serializable
data class PdfQuad(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

/**
 * A text selection reported by the viewer.
 *
 * [quads] holds one rectangle per line, since a selection running over several
 * lines would otherwise need a bounding box that also covers the gaps between them.
 */
@Serializable
data class TextSelection(
    @SerialName("page") val pageNumber: Int,
    val text: String,
    val quads: List<PdfQuad>
)

/**
 * A highlight as the viewer needs it in order to draw one.
 *
 * Deliberately narrower than the stored highlight: the text, label and note are of
 * no use to the renderer, so they are not sent across the bridge.
 *
 * @param color A CSS colour string. Semi-transparent, since the overlay paints over
 * the page.
 */
@Serializable
data class RenderedHighlight(
    val id: Long,
    @SerialName("page") val pageNumber: Int,
    val color: String,
    val quads: List<PdfQuad>
)
