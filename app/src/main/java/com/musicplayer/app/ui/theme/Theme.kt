package com.musicplayer.app.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.musicplayer.app.data.settings.DEFAULT_SEED_ARGB
import com.musicplayer.app.data.settings.PaletteStyle

/** Cute shapes: esquinas muy redondeadas (estilo Material 3 Expressive). */
val CuteShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return hsv
}

private fun colorFromHsv(hsv: FloatArray): Color = Color(AndroidColor.HSVToColor(hsv))

private fun Color.withHue(degrees: Float, satFactor: Float = 1f, valueFactor: Float = 1f): Color {
    val hsv = toHsv()
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    hsv[1] = (hsv[1] * satFactor).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * valueFactor).coerceIn(0f, 1f)
    return colorFromHsv(hsv)
}

private fun Color.saturate(factor: Float): Color = withHue(0f, factor)
private fun Color.desaturate(fraction: Float): Color = withHue(0f, 1f - fraction)
private fun Color.darken(valueFactor: Float): Color = withHue(0f, 1f, valueFactor)

/**
 * Esquema "cute" derivado de una semilla (tuerca HSV propia, sin HCT).
 * Tonos pastel en claro, tonos saturados sobre neutros en oscuro.
 */
private fun cuteColorScheme(dark: Boolean, seed: Color, style: PaletteStyle): ColorScheme {
    val primary = when (style) {
        PaletteStyle.MONSOON -> seed.desaturate(0.35f)
        PaletteStyle.EXPRESSIVE -> seed.saturate(1.15f)
        else -> seed
    }
    val secondaryHue = when (style) {
        PaletteStyle.FRUITSALAD -> -50f
        PaletteStyle.MONSOON -> 60f
        else -> 45f
    }
    val tertiaryHue = when (style) {
        PaletteStyle.MONSOON -> 160f
        else -> -35f
    }
    val secondary = primary.withHue(secondaryHue, 0.86f, if (dark) 1.06f else 0.92f)
    val tertiary = primary.withHue(tertiaryHue, 0.9f, if (dark) 1.05f else 0.9f)

    if (dark) {
        val bg = Color(0xFF100E14)
        val surface = Color(0xFF17131D)
        val surfaceHi = lerp(surface, primary, 0.14f)
        return darkColorScheme(
            primary = primary.darken(0.9f),
            onPrimary = bg,
            primaryContainer = primary.darken(0.62f),
            onPrimaryContainer = lerp(Color.White, primary, 0.18f),
            secondary = secondary,
            onSecondary = bg,
            secondaryContainer = secondary.darken(0.62f),
            onSecondaryContainer = lerp(Color.White, secondary, 0.22f),
            tertiary = tertiary,
            onTertiary = bg,
            tertiaryContainer = tertiary.darken(0.6f),
            onTertiaryContainer = lerp(Color.White, tertiary, 0.2f),
            background = bg,
            onBackground = Color(0xFFEFE9F6),
            surface = surface,
            onSurface = Color(0xFFEFE9F6),
            surfaceVariant = lerp(surface, primary, 0.16f),
            onSurfaceVariant = Color(0xFFCDC6D9),
            surfaceContainerLowest = Color(0xFF0B0810),
            surfaceContainerLow = lerp(surface, primary, 0.05f),
            surfaceContainer = surfaceHi,
            surfaceContainerHigh = lerp(surfaceHi, primary, 0.12f),
            surfaceContainerHighest = lerp(surfaceHi, primary, 0.2f),
            inverseSurface = Color(0xFFEFE9F6),
            inverseOnSurface = Color(0xFF2A2533),
            inversePrimary = primary,
            outline = Color(0xFF948EA3),
            outlineVariant = Color(0xFF473F55),
            scrim = Color.Black,
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    }

    val bg = lekker(Color.White, primary, 0.06f)
    return lightColorScheme(
        primary = primary.darken(0.82f),
        onPrimary = Color.White,
        primaryContainer = lekker(Color.White, primary, 0.2f),
        onPrimaryContainer = primary.darken(0.62f),
        secondary = secondary.darken(0.78f),
        onSecondary = Color.White,
        secondaryContainer = lekker(Color.White, secondary, 0.2f),
        onSecondaryContainer = secondary.darken(0.6f),
        tertiary = tertiary.darken(0.8f),
        onTertiary = Color.White,
        tertiaryContainer = lekker(Color.White, tertiary, 0.2f),
        onTertiaryContainer = tertiary.darken(0.58f),
        background = bg,
        onBackground = Color(0xFF22191F),
        surface = Color.White,
        onSurface = Color(0xFF22191F),
        surfaceVariant = lekker(Color.White, primary, 0.1f),
        onSurfaceVariant = Color(0xFF4C454C),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lekker(Color.White, primary, 0.04f),
        surfaceContainer = lekker(Color.White, primary, 0.07f),
        surfaceContainerHigh = lekker(Color.White, primary, 0.1f),
        surfaceContainerHighest = lekker(Color.White, primary, 0.14f),
        inverseSurface = Color(0xFF413A40),
        inverseOnSurface = Color.White,
        inversePrimary = lekker(Color.White, primary, 0.35f),
        outline = Color(0xFF7A7379),
        outlineVariant = Color(0xFFDDD5DC),
        scrim = Color.Black,
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002)
    )
}

private fun lekker(base: Color, accent: Color, factor: Float): Color = lerp(base, accent, factor)

@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    seedArgb: Long = DEFAULT_SEED_ARGB,
    paletteStyle: PaletteStyle = PaletteStyle.EXPRESSIVE,
    content: @Composable () -> Unit
) {
    val seed = Color(seedArgb)
    val colorScheme = remember(darkTheme, amoled, seedArgb, paletteStyle) {
        var scheme = cuteColorScheme(dark = darkTheme, seed = seed, style = paletteStyle)
        if (amoled && darkTheme) {
            val black = Color.Black
            scheme = scheme.copy(
                background = black,
                surface = black,
                surfaceContainerLowest = black,
                surfaceContainerLow = black
            )
        }
        scheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CuteShapes,
        content = content
    )
}