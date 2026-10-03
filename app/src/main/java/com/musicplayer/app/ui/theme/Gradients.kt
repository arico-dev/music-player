package com.musicplayer.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private fun mod(id: Long, size: Int): Int {
    val s = size.toLong()
    return (((id % s) + s) % s).toInt()
}

/**
 * Pares de color "cute" para portadas sin carátula: la misma id siempre produce
 * el mismo degradado, así cada álbum/playlist conserva su identidad de color.
 */
private val CoverGradientPairs = listOf(
    Color(0xFFFFC2D4) to Color(0xFFD81B60),
    Color(0xFFFFE0C2) to Color(0xFFF57C00),
    Color(0xFFC8F6D9) to Color(0xFF00897B),
    Color(0xFFC7E8FF) to Color(0xFF0277BD),
    Color(0xFFE4D3FF) to Color(0xFF6A1B9A),
    Color(0xFFFFD8E8) to Color(0xFFC2185B),
    Color(0xFFD3F7E9) to Color(0xFF00838F),
    Color(0xFFFFF1C1) to Color(0xFFFF8F00)
)

/** Degradado vertical determinista por [id] (misma id = mismos colores). */
fun coverGradient(id: Long): Brush =
    Brush.verticalGradient(
        CoverGradientPairs[mod(id, CoverGradientPairs.size)].let { (start, end) -> listOf(start, end) }
    )