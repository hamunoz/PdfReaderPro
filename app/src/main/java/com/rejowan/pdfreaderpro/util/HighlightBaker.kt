package com.rejowan.pdfreaderpro.util

import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfString
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.annot.PdfTextMarkupAnnotation
import com.rejowan.pdfreaderpro.domain.model.Highlight
import com.rejowan.pdfreaderpro.domain.model.HighlightQuad
import java.io.File
import java.io.OutputStream

/**
 * Writes stored highlights into a PDF as real text markup annotations, so they
 * survive in other readers.
 *
 * This is the opt-in half of the hybrid model. Room stays the source of truth for
 * the in-app experience; baking is a separate, explicit export.
 */
object HighlightBaker {

    /**
     * Copies [source] to [destination], adding a highlight annotation for every
     * entry in [highlights].
     *
     * Writes to a new stream rather than editing in place. iText cannot read and
     * write the same file at once, and a failure partway through would otherwise
     * leave the user's original truncated.
     *
     * @return the number of annotations written.
     */
    fun bake(source: File, destination: OutputStream, highlights: List<Highlight>): Int {
        var written = 0

        PdfReader(source).use { reader ->
            PdfWriter(destination).use { writer ->
                val document = PdfDocument(reader, writer)

                // Group by page so each page is only fetched once.
                highlights.groupBy { it.pageNumber }.forEach { (pageIndex, pageHighlights) ->
                    // Stored pages are 0-based, iText pages are 1-based.
                    val pageNumber = pageIndex + 1
                    if (pageNumber < 1 || pageNumber > document.numberOfPages) return@forEach

                    val page = document.getPage(pageNumber)
                    val box = page.cropBox ?: page.mediaBox

                    pageHighlights.forEach { highlight ->
                        if (highlight.quads.isEmpty()) return@forEach

                        val quadPoints = highlight.quads
                            .flatMap { quadPointsFor(it, box).asIterable() }
                            .toFloatArray()

                        val annotation = PdfTextMarkupAnnotation.createHighLight(
                            boundingBoxOf(highlight.quads, box),
                            quadPoints
                        ).apply {
                            setColor(highlight.color.toDeviceRgb())

                            // /Contents is the note attached to the markup, not a copy
                            // of the marked text. Writing the highlighted text here
                            // made every reader pop up a box repeating the words
                            // already under the highlight. Only a real note belongs
                            // here, and a highlight with neither note nor label gets
                            // no popup at all.
                            highlight.note?.takeIf { it.isNotBlank() }
                                ?.let { setContents(PdfString(it)) }
                            highlight.label?.takeIf { it.isNotBlank() }
                                ?.let { setTitle(PdfString(it)) }
                        }

                        page.addAnnotation(annotation)
                        written++
                    }
                }

                document.close()
            }
        }

        return written
    }

    /**
     * Converts a normalised quad into PDF user space quad points.
     *
     * PDF text markup expects eight floats in the order upper-left, upper-right,
     * lower-left, lower-right. Note that is not the order the corners are visited
     * when tracing the outline.
     */
    private fun quadPointsFor(quad: HighlightQuad, box: Rectangle): FloatArray {
        val left = box.left + quad.x * box.width
        val right = box.left + (quad.x + quad.w) * box.width
        // Stored coordinates use a top-left origin, PDF user space is bottom-up.
        val top = box.top - quad.y * box.height
        val bottom = box.top - (quad.y + quad.h) * box.height

        return floatArrayOf(left, top, right, top, left, bottom, right, bottom)
    }

    /** The rectangle enclosing every quad, which the annotation needs as its rect. */
    private fun boundingBoxOf(quads: List<HighlightQuad>, box: Rectangle): Rectangle {
        val lefts = quads.map { box.left + it.x * box.width }
        val rights = quads.map { box.left + (it.x + it.w) * box.width }
        val tops = quads.map { box.top - it.y * box.height }
        val bottoms = quads.map { box.top - (it.y + it.h) * box.height }

        val left = lefts.min()
        val bottom = bottoms.min()

        return Rectangle(left, bottom, rights.max() - left, tops.max() - bottom)
    }

    private fun Int.toDeviceRgb(): DeviceRgb =
        DeviceRgb((this shr 16) and 0xFF, (this shr 8) and 0xFF, this and 0xFF)
}
