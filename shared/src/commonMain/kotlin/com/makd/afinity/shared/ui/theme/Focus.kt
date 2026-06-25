package com.makd.afinity.shared.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * tvOS-style focus affordance: grow the element slightly and draw a subtle
 * accent ring while it holds focus.
 *
 * Apply at the focus boundary of a card/button. Composable because the ring
 * pulls its color from [MaterialTheme.colorScheme] (primary).
 *
 * @param focused whether the element currently holds focus
 * @param scale grow factor when focused (default ~1.06, the design-language value)
 * @param ring whether to draw the accent border ring when focused
 * @param ringWidth thickness of the focus ring
 * @param shape corner shape for the ring; match the element's own clip
 */
@Composable
fun Modifier.tvFocusScale(
    focused: Boolean,
    scale: Float = 1.06f,
    ring: Boolean = true,
    ringWidth: Dp = 2.dp,
    shape: RoundedCornerShape = MaterialTheme.shapes.medium as? RoundedCornerShape
        ?: RoundedCornerShape(14.dp),
): Modifier {
    val scaled = this.scale(if (focused) scale else 1f)
    return if (focused && ring) {
        scaled.border(ringWidth, MaterialTheme.colorScheme.primary, shape)
    } else {
        scaled
    }
}
