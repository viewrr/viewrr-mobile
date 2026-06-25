package com.makd.afinity.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Apple-TV-flavored design tokens for viewrr.
 *
 * Dark, cinematic palette: near-black backgrounds, layered surfaces, a vibrant
 * tvOS-blue accent, high-contrast near-white text. Pair with [tvFocusScale] for
 * the focus-grow + ring interaction that defines the design language.
 */
object ViewrrColors {
    /** App backdrop — near-black with a faint cool tint. */
    val Background = Color(0xFF0A0A0F)

    /** Primary card/sheet surface, one step above the backdrop. */
    val Surface = Color(0xFF15151C)

    /** Elevated/inset surface (poster placeholders, chips). */
    val SurfaceVariant = Color(0xFF23232E)

    /** Vibrant tvOS-blue accent for focus rings, highlights, key actions. */
    val Primary = Color(0xFF5B8CFF)

    /** Foreground on the accent — near-black for crisp contrast on bright blue. */
    val OnPrimary = Color(0xFF06060A)

    /** Near-white primary text on dark backgrounds. */
    val OnBackground = Color(0xFFF4F5F8)

    /** Near-white text on surfaces. */
    val OnSurface = Color(0xFFF4F5F8)

    /** Muted text on the inset surface variant. */
    val OnSurfaceVariant = Color(0xFFB6B8C4)

    /** Standard Material error red, tuned for dark UIs. */
    val Error = Color(0xFFFF6B6B)

    /** Foreground on error. */
    val OnError = Color(0xFF06060A)
}

private val ViewrrColorScheme =
    darkColorScheme(
        primary = ViewrrColors.Primary,
        onPrimary = ViewrrColors.OnPrimary,
        background = ViewrrColors.Background,
        onBackground = ViewrrColors.OnBackground,
        surface = ViewrrColors.Surface,
        onSurface = ViewrrColors.OnSurface,
        surfaceVariant = ViewrrColors.SurfaceVariant,
        onSurfaceVariant = ViewrrColors.OnSurfaceVariant,
        error = ViewrrColors.Error,
        onError = ViewrrColors.OnError,
    )

/**
 * TV-tuned shapes — generous rounding for the cinematic, soft-cornered look.
 */
private val ViewrrShapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
    )

/**
 * Typography tuned for TV cards and titles: bold display/headline weights for
 * across-the-room legibility, readable body. Built by bumping a few weights on
 * the Material3 defaults so it stays commonMain-safe (no font resources).
 */
private val ViewrrTypography: Typography
    @Composable
    get() {
        val base = MaterialTheme.typography
        return base.copy(
            displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold),
            displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold),
            displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
            headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }

/**
 * Apple-TV-flavored Material3 theme for viewrr. Wrap screens that should adopt
 * the dark, cinematic look in place of a bare `MaterialTheme {}`.
 */
@Composable
fun ViewrrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ViewrrColorScheme,
        typography = ViewrrTypography,
        shapes = ViewrrShapes,
        content = content,
    )
}
