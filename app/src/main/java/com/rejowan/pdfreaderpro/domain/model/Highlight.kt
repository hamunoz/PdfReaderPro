package com.rejowan.pdfreaderpro.domain.model

import kotlinx.serialization.Serializable

/**
 * One rectangle of a highlighted region, normalised to 0..1 against the unrotated
 * page box.
 *
 * Storing normalised coordinates rather than pixels means a highlight stays put
 * across zoom levels, page rotation, and device rotation, with no recalculation on
 * our side. The renderer multiplies these back up by the page's current dimensions.
 */
@Serializable
data class HighlightQuad(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

/** Where a highlight came from, which decides whether it can be edited. */
enum class HighlightSource {
    /** Created in this app and stored in its database. Fully editable. */
    APP,

    /**
     * Already present in the PDF file, either baked by us earlier or added by
     * another application. Listed, navigable and searchable, but read only, since
     * editing it would mean rewriting the user's document.
     */
    DOCUMENT
}

/**
 * A text highlight.
 *
 * A selection spanning more than one line produces one quad per line, which is why
 * [quads] is a list rather than a single rectangle.
 */
data class Highlight(
    val id: Long = 0,
    val pdfPath: String,
    val pageNumber: Int,
    val text: String,
    val quads: List<HighlightQuad>,
    val color: Int,
    val label: String? = null,
    val note: String? = null,
    val sortIndex: Int = 0,
    val source: HighlightSource = HighlightSource.APP,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Only highlights the app owns can be recoloured or deleted. */
    val isEditable: Boolean get() = source == HighlightSource.APP

    /** Single-line preview for the highlights panel, where rows are one line tall. */
    val preview: String
        get() = text.replace(WHITESPACE_RUN, " ").trim()

    companion object {
        val WHITESPACE_RUN = Regex("\\s+")
    }
}
