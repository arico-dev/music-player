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
import kotlin.math.pow

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
            surfaceDim = Color(0xFF0A070E),
            surfaceBright = Color(0xFF2C2636),
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
        surfaceDim = lerp(bg, Color.Black, 0.10f),
        surfaceBright = Color.White,
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

/* ---- Garantía de contraste (WCAG AA) -------------------------------------
 * Los esquemas derivados por HSV dependen de la semilla y del estilo: sin este
 * ajuste, semillas claras producían botones con texto invisible (claro) o
 * acentos casi negros sobre fondo oscuro (el "botón Reproducir" del issue #2).
 * Se calcula una sola vez al reconstruir el tema (sin coste por frame).
 */

private fun linearize(channel: Float): Double {
    val c = channel.toDouble()
    return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

private fun relativeLuminance(c: Color): Double =
    0.2126 * linearize(c.red) + 0.7152 * linearize(c.green) + 0.0722 * linearize(c.blue)

private fun contrastRatio(a: Color, b: Color): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return ((hi + 0.05) / (lo + 0.05)).toFloat()
}

/** Blanco o negro: la mejor tinta para [bg]. */
private fun bestContent(bg: Color): Color =
    if (contrastRatio(Color.White, bg) >= contrastRatio(Color.Black, bg)) Color.White else Color.Black

/**
 * Lleva [text] hasta [target] de contraste contra [bg]. Se decide la dirección
 * UNA vez (el extremo —negro o blanco— que mejor contraste final dé) y se avanza
 * monótonamente hacia él: un paso greedy podría estancarse en la zona intermedia
 * de bajo contraste. Para cualquier fondo, negro o blanco alcanzan 4.5:1.
 */
private fun hardenText(text: Color, bg: Color, target: Float): Color {
    var t = text
    if (contrastRatio(t, bg) >= target) return t
    val extreme = if (contrastRatio(Color.Black, bg) >= contrastRatio(Color.White, bg)) {
        Color.Black
    } else {
        Color.White
    }
    repeat(16) {
        if (contrastRatio(t, bg) >= target) return t
        val next = lerp(t, extreme, 0.6f)
        if (next == t) return t
        t = next
    }
    return t
}

/**
 * Ajusta el brillo (valor HSV, conservando tono/saturación "cute") de un color
 * de acento hasta que contraste [target] contra [bg]. Fondo claro → oscurece;
 * fondo oscuro → aclara. Si por HSV no llega, empuja en blanco/negro.
 */
private fun liftColor(color: Color, bg: Color, target: Float): Color {
    var c = color
    if (contrastRatio(c, bg) >= target) return c
    val darken = relativeLuminance(bg) > 0.25
    repeat(14) {
        if (contrastRatio(c, bg) >= target) return c
        val hsv = c.toHsv()
        val nv = if (darken) hsv[2] * 0.88f else (hsv[2] + 0.07f).coerceAtMost(1f)
        val next = colorFromHsv(floatArrayOf(hsv[0], hsv[1], nv))
        if (next == c) return hardenText(c, bg, target)
        c = next
    }
    return hardenText(c, bg, target)
}

private fun lightestOf(colors: List<Color>): Color = colors.maxByOrNull { relativeLuminance(it) }!!
private fun darkestOf(colors: List<Color>): Color = colors.minByOrNull { relativeLuminance(it) }!!

/**
 * Endurece el esquema completo: acentos visibles sobre la página (≥3:1),
 * etiquetas de botón legibles (≥4.5:1) y textos sobre las superficies
 * más clara y más oscura (≥7:1 / ≥6:1 en metadatos).
 */
private fun enforceContrast(scheme: ColorScheme): ColorScheme {
    val bg = scheme.background

    val primary = liftColor(scheme.primary, bg, 3.0f)
    val secondary = liftColor(scheme.secondary, bg, 3.0f)
    val tertiary = liftColor(scheme.tertiary, bg, 3.0f)

    // Superficies candidatas: los textos deben aguantar la más desfavorable.
    val surfaces = listOf(
        bg, scheme.surface, scheme.surfaceVariant,
        scheme.surfaceContainerLowest, scheme.surfaceContainerLow,
        scheme.surfaceContainer, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest
    )
    val lightest = lightestOf(surfaces)
    val darkest = darkestOf(surfaces)
    fun textOnSurfaces(text: Color, target: Float): Color =
        hardenText(hardenText(text, lightest, target), darkest, target)

    return scheme.copy(
        primary = primary,
        onPrimary = hardenText(bestContent(primary), primary, 4.5f),
        secondary = secondary,
        onSecondary = hardenText(bestContent(secondary), secondary, 4.5f),
        tertiary = tertiary,
        onTertiary = hardenText(bestContent(tertiary), tertiary, 4.5f),
        onPrimaryContainer = hardenText(scheme.onPrimaryContainer, scheme.primaryContainer, 4.5f),
        onSecondaryContainer = hardenText(scheme.onSecondaryContainer, scheme.secondaryContainer, 4.5f),
        onTertiaryContainer = hardenText(scheme.onTertiaryContainer, scheme.tertiaryContainer, 4.5f),
        onBackground = textOnSurfaces(scheme.onBackground, 7.0f),
        onSurface = textOnSurfaces(scheme.onSurface, 7.0f),
        onSurfaceVariant = textOnSurfaces(scheme.onSurfaceVariant, 6.0f),
        inversePrimary = hardenText(scheme.inversePrimary, scheme.inverseSurface, 3.0f),
        inverseOnSurface = hardenText(scheme.inverseOnSurface, scheme.inverseSurface, 4.5f),
        onErrorContainer = hardenText(scheme.onErrorContainer, scheme.errorContainer, 4.5f),
        outline = hardenText(scheme.outline, bg, 3.0f)
    )
}

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
            // AMOLED de verdad: negro puro en todas las superficies (antes solo
            // background/surface) con contenedores apenas elevados para que las
            // filas de listas floten sobre #000000 sin paneles grises.
            val black = Color.Black
            scheme = scheme.copy(
                background = black,
                surface = black,
                surfaceDim = black,
                surfaceVariant = Color(0xFF141116),
                surfaceContainerLowest = black,
                surfaceContainerLow = Color(0xFF050505),
                surfaceContainer = Color(0xFF0A0A0B),
                surfaceContainerHigh = Color(0xFF0F0F12),
                surfaceContainerHighest = Color(0xFF151519),
                surfaceBright = Color(0xFF16161A)
            )
        }
        enforceContrast(scheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CuteShapes,
        content = content
    )
}