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
/**
 * Where a selection sits within the viewer, in CSS pixels relative to its top-left.
 *
 * Viewer coordinates rather than the normalised page coordinates [PdfQuad] uses,
 * because this exists to anchor app UI over the viewer, not to survive zoom.
 */
@Serializable
data class SelectionAnchor(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

/**
 * A Highlight annotation the PDF file already carries.
 *
 * @param id Negative, to stay clear of the app's own positive database ids.
 * @param color ARGB, or -1 when the annotation records no colour.
 */
@Serializable
data class DocumentHighlight(
    val id: Long,
    @SerialName("page") val pageNumber: Int,
    val color: Int,
    val text: String = "",
    val note: String = "",
    val label: String = "",
    val quads: List<PdfQuad> = emptyList()
)

/** A tapped highlight and where it sits, so app UI can anchor to it. */
@Serializable
data class TappedHighlight(
    val id: Long,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
) {
    val anchor: SelectionAnchor get() = SelectionAnchor(x, y, w, h)
}

@Serializable
data class TextSelection(
    @SerialName("page") val pageNumber: Int,
    val text: String,
    val quads: List<PdfQuad>,
    val anchor: SelectionAnchor? = null
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
