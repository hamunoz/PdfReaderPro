package com.rejowan.pdfreaderpro.presentation.screens.reader

/**
 * The highlight palette.
 *
 * Matches the viewer's own highlight editor colours (see
 * `getHighlightEditorColorsString` in jwi_interface.js) so a highlight looks the
 * same whether it was drawn by us or baked into the PDF later.
 *
 * Stored at full opacity. The overlay applies [HIGHLIGHT_FILL_ALPHA] when painting,
 * since it draws over the page rather than behind it.
 */
object HighlightColors {

    const val YELLOW = 0xFFFFFF98.toInt()
    const val GREEN = 0xFF53FFBC.toInt()
    const val BLUE = 0xFF80EBFF.toInt()
    const val PINK = 0xFFFFCBE6.toInt()
    const val RED = 0xFFFF4F5F.toInt()

    val ALL = listOf(YELLOW, GREEN, BLUE, PINK, RED)

    val DEFAULT = YELLOW
}

/**
 * Opacity used when painting a highlight over the page.
 *
 * The overlay also blends with multiply, so this only needs to be low enough to
 * keep the fill from flattening the glyphs underneath.
 */
const val HIGHLIGHT_FILL_ALPHA = 0.40f
