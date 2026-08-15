package com.rejowan.pdfreaderpro.presentation.components.pdf.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Locks the wire format between `getSelectionInfo()` in helper_methods.js and the
 * Kotlin side. The JSON here is written to match exactly what that function emits,
 * so a change to either end without the other fails here.
 */
class TextSelectionTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a single line selection as the viewer emits it`() {
        val raw = """{"page":12,"text":"concentration","quads":[{"x":0.1,"y":0.2,"w":0.3,"h":0.02}]}"""

        val result = json.decodeFromString<TextSelection>(raw)

        assertEquals(12, result.pageNumber)
        assertEquals("concentration", result.text)
        assertEquals(listOf(PdfQuad(0.1f, 0.2f, 0.3f, 0.02f)), result.quads)
    }

    @Test
    fun `decodes one quad per line for a multi-line selection`() {
        val raw = """
            {"page":3,"text":"concentration gradient","quads":[
              {"x":0.60,"y":0.20,"w":0.30,"h":0.02},
              {"x":0.10,"y":0.23,"w":0.25,"h":0.02}
            ]}
        """.trimIndent()

        val result = json.decodeFromString<TextSelection>(raw)

        assertEquals(2, result.quads.size)
        assertEquals(0.23f, result.quads[1].y)
    }

    @Test
    fun `maps the page key onto pageNumber`() {
        val raw = """{"page":7,"text":"a","quads":[]}"""
        assertEquals(7, json.decodeFromString<TextSelection>(raw).pageNumber)
    }

    @Test
    fun `keeps newlines in the selected text`() {
        val raw = """{"page":1,"text":"first\nsecond","quads":[]}"""
        assertEquals("first\nsecond", json.decodeFromString<TextSelection>(raw).text)
    }

    @Test
    fun `tolerates extra keys so the viewer can add fields without breaking us`() {
        val raw = """{"page":1,"text":"a","quads":[],"rotation":90}"""
        assertEquals(1, json.decodeFromString<TextSelection>(raw).pageNumber)
    }

    @Test
    fun `rejects a payload missing required fields`() {
        assertThrows(Exception::class.java) {
            json.decodeFromString<TextSelection>("""{"page":1}""")
        }
    }

    @Test
    fun `round trips`() {
        val original = TextSelection(
            pageNumber = 4,
            text = "osmotic pressure",
            quads = listOf(PdfQuad(0.1f, 0.2f, 0.3f, 0.02f))
        )

        assertEquals(original, json.decodeFromString<TextSelection>(json.encodeToString(original)))
    }
}
