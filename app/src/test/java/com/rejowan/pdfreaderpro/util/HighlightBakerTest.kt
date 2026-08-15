package com.rejowan.pdfreaderpro.util

import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.annot.PdfTextMarkupAnnotation
import com.rejowan.pdfreaderpro.domain.model.Highlight
import com.rejowan.pdfreaderpro.domain.model.HighlightQuad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Round-trips real PDFs through the baker and reads the annotations back, so the
 * quad point maths is checked against what iText actually writes rather than
 * against a restatement of the same formula.
 */
class HighlightBakerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun sourcePdf(pages: Int = 3, size: PageSize = PageSize.A4): File {
        val file = folder.newFile("source-${pages}-${size.width.toInt()}.pdf")
        PdfDocument(PdfWriter(file)).use { document ->
            repeat(pages) { document.addNewPage(size) }
        }
        return file
    }

    private fun highlight(
        page: Int = 0,
        quads: List<HighlightQuad> = listOf(HighlightQuad(0.1f, 0.2f, 0.3f, 0.02f)),
        color: Int = 0xFFFFFF98.toInt(),
        text: String = "concentration gradient",
        label: String? = null
    ) = Highlight(
        id = 1,
        pdfPath = "/tmp/a.pdf",
        pageNumber = page,
        text = text,
        quads = quads,
        color = color,
        label = label
    )

    private fun bakeAndRead(source: File, highlights: List<Highlight>): PdfDocument {
        val out = ByteArrayOutputStream()
        HighlightBaker.bake(source, out, highlights)
        val baked = folder.newFile("baked-${System.nanoTime()}.pdf")
        baked.writeBytes(out.toByteArray())
        return PdfDocument(PdfReader(baked))
    }

    @Test
    fun `bake reports how many annotations it wrote`() {
        val out = ByteArrayOutputStream()

        val written = HighlightBaker.bake(
            sourcePdf(),
            out,
            listOf(highlight(page = 0), highlight(page = 1), highlight(page = 2))
        )

        assertEquals(3, written)
    }

    @Test
    fun `annotations land on the right pages`() {
        bakeAndRead(sourcePdf(), listOf(highlight(page = 0), highlight(page = 2))).use { doc ->
            assertEquals(1, doc.getPage(1).annotations.size)
            assertEquals(0, doc.getPage(2).annotations.size)
            assertEquals(1, doc.getPage(3).annotations.size)
        }
    }

    @Test
    fun `annotations are written as highlight text markup`() {
        bakeAndRead(sourcePdf(), listOf(highlight())).use { doc ->
            val annotation = doc.getPage(1).annotations.single()
            assertEquals(PdfName.Highlight, annotation.subtype)
            assertTrue(annotation is PdfTextMarkupAnnotation)
        }
    }

    @Test
    fun `quad points map onto the page box`() {
        // A4 is 595.276 x 841.89. A quad at x=0.1 w=0.3, y=0.2 h=0.02 should span
        // 10%..40% across and 20%..22% down from the top.
        bakeAndRead(sourcePdf(), listOf(highlight())).use { doc ->
            val page = doc.getPage(1)
            val quads = page.annotations.single().pdfObject
                .getAsArray(PdfName.QuadPoints)
                .toFloatArray()

            val width = page.cropBox.width
            val height = page.cropBox.height

            assertEquals(0.1f * width, quads[0], 0.01f)   // upper-left x
            assertEquals(height - 0.2f * height, quads[1], 0.01f)  // upper-left y
            assertEquals(0.4f * width, quads[2], 0.01f)   // upper-right x
            assertEquals(height - 0.22f * height, quads[5], 0.01f) // lower-left y
        }
    }

    @Test
    fun `quad point order is upper-left upper-right lower-left lower-right`() {
        bakeAndRead(sourcePdf(), listOf(highlight())).use { doc ->
            val quads = doc.getPage(1).annotations.single().pdfObject
                .getAsArray(PdfName.QuadPoints)
                .toFloatArray()

            assertEquals(8, quads.size)
            // Both top corners share a y, both bottom corners share a lower y.
            assertEquals(quads[1], quads[3], 0.001f)
            assertEquals(quads[5], quads[7], 0.001f)
            assertTrue("top must be above bottom", quads[1] > quads[5])
            // Left corners share an x, right corners share a larger x.
            assertEquals(quads[0], quads[4], 0.001f)
            assertEquals(quads[2], quads[6], 0.001f)
            assertTrue("left must be left of right", quads[2] > quads[0])
        }
    }

    @Test
    fun `a multi-line highlight writes one quad per line`() {
        val multiLine = highlight(
            quads = listOf(
                HighlightQuad(0.60f, 0.20f, 0.30f, 0.02f),
                HighlightQuad(0.10f, 0.23f, 0.25f, 0.02f)
            )
        )

        bakeAndRead(sourcePdf(), listOf(multiLine)).use { doc ->
            val quads = doc.getPage(1).annotations.single().pdfObject
                .getAsArray(PdfName.QuadPoints)
                .toFloatArray()

            assertEquals(16, quads.size)
        }
    }

    @Test
    fun `the bounding box encloses every quad of a multi-line highlight`() {
        val multiLine = highlight(
            quads = listOf(
                HighlightQuad(0.60f, 0.20f, 0.30f, 0.02f),
                HighlightQuad(0.10f, 0.23f, 0.25f, 0.02f)
            )
        )

        bakeAndRead(sourcePdf(), listOf(multiLine)).use { doc ->
            val page = doc.getPage(1)
            val rect = page.annotations.single().rectangle.toRectangle()
            val width = page.cropBox.width
            val height = page.cropBox.height

            // Spans the leftmost quad's left edge to the rightmost quad's right edge.
            assertEquals(0.10f * width, rect.left, 0.01f)
            assertEquals(0.90f * width, rect.right, 0.01f)
            // And from the highest top down to the lowest bottom.
            assertEquals(height - 0.20f * height, rect.top, 0.01f)
            assertEquals(height - 0.25f * height, rect.bottom, 0.01f)
        }
    }

    @Test
    fun `the highlight colour is preserved`() {
        bakeAndRead(sourcePdf(), listOf(highlight(color = 0xFF53FFBC.toInt()))).use { doc ->
            val color = doc.getPage(1).annotations.single().colorObject.toFloatArray()

            assertEquals(0x53 / 255f, color[0], 0.01f)
            assertEquals(0xFF / 255f, color[1], 0.01f)
            assertEquals(0xBC / 255f, color[2], 0.01f)
        }
    }

    /**
     * /Contents is the note attached to the markup, not a copy of the marked text.
     * Writing the highlighted text here made readers pop up a box repeating the words
     * already visible under the highlight.
     */
    @Test
    fun `the highlighted text is not copied into the annotation contents`() {
        bakeAndRead(sourcePdf(), listOf(highlight(text = "osmotic pressure"))).use { doc ->
            assertNull(doc.getPage(1).annotations.single().contents)
        }
    }

    @Test
    fun `a note becomes the annotation contents`() {
        val withNote = highlight().copy(note = "revisit before the exam")
        bakeAndRead(sourcePdf(), listOf(withNote)).use { doc ->
            assertEquals(
                "revisit before the exam",
                doc.getPage(1).annotations.single().contents.toUnicodeString()
            )
        }
    }

    @Test
    fun `a blank note is left off rather than written as empty`() {
        val blankNote = highlight().copy(note = "   ")
        bakeAndRead(sourcePdf(), listOf(blankNote)).use { doc ->
            assertNull(doc.getPage(1).annotations.single().contents)
        }
    }

    /** No note and no label means no popup in any reader. */
    @Test
    fun `a plain highlight carries neither contents nor title`() {
        bakeAndRead(sourcePdf(), listOf(highlight())).use { doc ->
            val annotation = doc.getPage(1).annotations.single()
            assertNull(annotation.contents)
            assertNull(annotation.title)
        }
    }

    @Test
    fun `a label becomes the annotation title`() {
        bakeAndRead(sourcePdf(), listOf(highlight(label = "Important"))).use { doc ->
            assertEquals("Important", doc.getPage(1).annotations.single().title.toUnicodeString())
        }
    }

    @Test
    fun `the page count is unchanged`() {
        bakeAndRead(sourcePdf(pages = 5), listOf(highlight(page = 1))).use { doc ->
            assertEquals(5, doc.numberOfPages)
        }
    }

    @Test
    fun `quads scale to a non-A4 page`() {
        val letter = sourcePdf(pages = 1, size = PageSize.LETTER)

        bakeAndRead(letter, listOf(highlight())).use { doc ->
            val page = doc.getPage(1)
            val quads = page.annotations.single().pdfObject
                .getAsArray(PdfName.QuadPoints)
                .toFloatArray()

            assertEquals(0.1f * page.cropBox.width, quads[0], 0.01f)
        }
    }

    @Test
    fun `a highlight on a page beyond the end is skipped rather than throwing`() {
        val out = ByteArrayOutputStream()

        val written = HighlightBaker.bake(
            sourcePdf(pages = 2),
            out,
            listOf(highlight(page = 0), highlight(page = 99))
        )

        assertEquals(1, written)
    }

    @Test
    fun `a highlight with no quads is skipped`() {
        val out = ByteArrayOutputStream()

        val written = HighlightBaker.bake(
            sourcePdf(),
            out,
            listOf(highlight(quads = emptyList()))
        )

        assertEquals(0, written)
    }

    @Test
    fun `baking nothing still produces a readable pdf`() {
        bakeAndRead(sourcePdf(pages = 2), emptyList()).use { doc ->
            assertEquals(2, doc.numberOfPages)
            assertEquals(0, doc.getPage(1).annotations.size)
        }
    }

    @Test
    fun `several highlights on one page are all written`() {
        val highlights = listOf(
            highlight(page = 1, quads = listOf(HighlightQuad(0.1f, 0.2f, 0.3f, 0.02f))),
            highlight(page = 1, quads = listOf(HighlightQuad(0.1f, 0.3f, 0.3f, 0.02f))),
            highlight(page = 1, quads = listOf(HighlightQuad(0.1f, 0.4f, 0.3f, 0.02f)))
        )

        bakeAndRead(sourcePdf(), highlights).use { doc ->
            assertEquals(3, doc.getPage(2).annotations.size)
        }
    }
}
