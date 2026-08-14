package com.rejowan.pdfreaderpro.presentation.screens.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.presentation.screens.reader.HighlightColors

private val SWATCH_SIZE = 32.dp
private val PICKER_CORNER = 20.dp

/**
 * Colour swatches for a new or existing highlight.
 *
 * A compact floating bar rather than a sheet: it appears straight after a text
 * selection and is dismissed by picking a colour, so a modal would be heavier than
 * the interaction warrants.
 *
 * @param selectedColor The current colour when editing, `null` when creating.
 * @param onDelete Shown only when editing an existing highlight.
 */
@Composable
fun HighlightColorPicker(
    isVisible: Boolean,
    selectedColor: Int?,
    onColorSelected: (Int) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(PICKER_CORNER),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HighlightColors.ALL.forEach { color ->
                    ColorSwatch(
                        color = color,
                        isSelected = color == selectedColor,
                        onClick = { onColorSelected(color) }
                    )
                }

                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(SWATCH_SIZE)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Box(
                        modifier = Modifier
                            .size(SWATCH_SIZE)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.delete_highlight),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val swatchColor = Color(color)
    val description = stringResource(colorNameFor(color))

    Box(
        modifier = Modifier
            .size(SWATCH_SIZE)
            .clip(CircleShape)
            .background(swatchColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                // The palette is light by design, so a dark tick stays legible on
                // every swatch.
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun colorNameFor(color: Int): Int = when (color) {
    HighlightColors.YELLOW -> R.string.highlight_color_yellow
    HighlightColors.GREEN -> R.string.highlight_color_green
    HighlightColors.BLUE -> R.string.highlight_color_blue
    HighlightColors.PINK -> R.string.highlight_color_pink
    HighlightColors.RED -> R.string.highlight_color_red
    else -> R.string.highlight_color
}
