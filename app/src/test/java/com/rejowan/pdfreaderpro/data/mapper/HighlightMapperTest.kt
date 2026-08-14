package com.rejowan.pdfreaderpro.data.mapper

import com.rejowan.pdfreaderpro.data.local.database.entity.AnnotationEntity
import com.rejowan.pdfreaderpro.domain.model.Highlight
import com.rejowan.pdfreaderpro.domain.model.HighlightQuad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for highlight quad serialisation and entity mapping.
 *
 * The quad JSON is the one place a stored highlight can be malformed, since it is
 * written from the JS side, so the decode path has to degrade rather than throw.
 */
class HighlightMapperTest {

    // region decodeQuads

    @Test
    fun `decodeQuads parses a single quad`() {
        val result = decodeQuads("""[{"x":0.1,"y":0.2,"w":0.3,"h":0.02}]""")
        assertEquals(listOf(HighlightQuad(0.1f, 0.2f, 0.3f, 0.02f)), result)
    }

    @Test
    fun `decodeQuads parses a multi-line selection`() {
        val result = decodeQuads(
            """[{"x":0.1,"y":0.2,"w":0.8,"h":0.02},{"x":0.1,"y":0.23,"w":0.4,"h":0.02}]"""
        )
        assertEquals(2, result.size)
        assertEquals(0.23f, result[1].y)
    }

    @Test
    fun `decodeQuads returns empty for null`() {
        assertTrue(decodeQuads(null).isEmpty())
    }

    @Test
    fun `decodeQuads returns empty for blank`() {
        assertTrue(decodeQuads("   ").isEmpty())
    }

    @Test
    fun `decodeQuads returns empty for malformed json rather than throwing`() {
        assertTrue(decodeQuads("not json at all").isEmpty())
        assertTrue(decodeQuads("""[{"x":0.1,""").isEmpty())
    }

    @Test
    fun `decodeQuads returns empty when the shape is wrong`() {
        assertTrue(decodeQuads("""{"x":0.1,"y":0.2,"w":0.3,"h":0.02}""").isEmpty())
        assertTrue(decodeQuads("""[{"left":0.1}]""").isEmpty())
    }

    @Test
    fun `decodeQuads ignores unknown keys`() {
        val result = decodeQuads("""[{"x":0.1,"y":0.2,"w":0.3,"h":0.02,"page":4}]""")
        assertEquals(listOf(HighlightQuad(0.1f, 0.2f, 0.3f, 0.02f)), result)
    }

    // endregion

    // region round trip

    @Test
    fun `encode then decode round trips`() {
        val quads = listOf(
            HighlightQuad(0.1f, 0.2f, 0.8f, 0.02f),
            HighlightQuad(0.1f, 0.23f, 0.45f, 0.02f)
        )
        assertEquals(quads, decodeQuads(encodeQuads(quads)))
    }

    @Test
    fun `highlight survives a full entity round trip`() {
        val original = Highlight(
            id = 7,
            pdfPath = "/storage/emulated/0/Documents/biology.pdf",
            pageNumber = 12,
            text = "concentration gradient",
            quads = listOf(HighlightQuad(0.1f, 0.2f, 0.3f, 0.02f)),
            color = 0xFFFFFF98.toInt(),
            label = "Important",
            note = "revisit before the exam",
            sortIndex = 2,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertEquals(original, original.toEntity().toHighlight())
    }

    @Test
    fun `toEntity marks the row as a highlight`() {
        val entity = highlight().toEntity()
        assertEquals(ANNOTATION_TYPE_HIGHLIGHT, entity.type)
    }

    // endregion

    // region entity to domain

    @Test
    fun `toHighlight falls back to the default colour when none is stored`() {
        val result = entity(color = null).toHighlight()
        assertEquals(0xFFFFFF98.toInt(), result.color)
    }

    @Test
    fun `toHighlight tolerates a row with no text`() {
        assertEquals("", entity(selectedText = null).toHighlight().text)
    }

    @Test
    fun `toHighlight keeps the text when the quads are corrupt`() {
        val result = entity(quads = "{{{ broken").toHighlight()
        assertEquals("concentration gradient", result.text)
        assertTrue(result.quads.isEmpty())
    }

    // endregion

    // region preview

    @Test
    fun `preview collapses newlines from a multi-line selection`() {
        val result = highlight(text = "concentration\ngradient").preview
        assertEquals("concentration gradient", result)
    }

    @Test
    fun `preview collapses runs of whitespace and trims`() {
        assertEquals("a b", highlight(text = "  a   \n\t b  ").preview)
    }

    // endregion

    // region asOpaqueSwatch

    @Test
    fun `asOpaqueSwatch forces full alpha and keeps the rgb channels`() {
        assertEquals(0xFFFFFF98.toInt(), 0x40FFFF98.toInt().asOpaqueSwatch())
    }

    @Test
    fun `asOpaqueSwatch leaves an already opaque colour unchanged`() {
        assertEquals(0xFF53FFBC.toInt(), 0xFF53FFBC.toInt().asOpaqueSwatch())
    }

    // endregion

    private fun highlight(text: String = "concentration gradient") = Highlight(
        pdfPath = "/tmp/a.pdf",
        pageNumber = 1,
        text = text,
        quads = emptyList(),
        color = 0xFFFFFF98.toInt()
    )

    private fun entity(
        selectedText: String? = "concentration gradient",
        quads: String? = null,
        color: Int? = 0xFFFFFF98.toInt()
    ) = AnnotationEntity(
        pdfPath = "/tmp/a.pdf",
        pageNumber = 1,
        type = ANNOTATION_TYPE_HIGHLIGHT,
        content = null,
        color = color,
        selectedText = selectedText,
        quads = quads
    )

    // region toRendered

    /**
     * Highlights are stored 0-based but the viewer indexes pages 1-based. Getting
     * this wrong draws every highlight one page early, which nothing in the data
     * layer reveals and only shows up on a device.
     */
    @Test
    fun `toRendered converts the page number to 1-based`() {
        val result = highlight().copy(pageNumber = 282).toRendered(0.4f)
        assertEquals(283, result.pageNumber)
    }

    @Test
    fun `toRendered maps the first page to page 1`() {
        assertEquals(1, highlight().copy(pageNumber = 0).toRendered(0.4f).pageNumber)
    }

    @Test
    fun `toRendered builds a css rgba colour with the given alpha`() {
        val result = highlight().copy(color = 0xFF53FFBC.toInt()).toRendered(0.4f)
        assertEquals("rgba(83, 255, 188, 0.4)", result.color)
    }

    @Test
    fun `toRendered drops the stored alpha in favour of the fill alpha`() {
        // Stored fully opaque, but the overlay paints over the page.
        val result = highlight().copy(color = 0xFFFFFF98.toInt()).toRendered(0.25f)
        assertEquals("rgba(255, 255, 152, 0.25)", result.color)
    }

    @Test
    fun `toRendered carries the id across`() {
        assertEquals(7L, highlight().copy(id = 7L).toRendered(0.4f).id)
    }

    @Test
    fun `toRendered carries every quad across unchanged`() {
        val quads = listOf(
            HighlightQuad(0.60f, 0.20f, 0.30f, 0.02f),
            HighlightQuad(0.10f, 0.23f, 0.25f, 0.02f)
        )

        val result = highlight().copy(quads = quads).toRendered(0.4f)

        assertEquals(2, result.quads.size)
        assertEquals(0.60f, result.quads[0].x)
        assertEquals(0.23f, result.quads[1].y)
    }
    // endregion
}
