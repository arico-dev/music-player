package com.musicplayer.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía basada en la escala M3 por defecto pero con pesos más densos
 * (SemiBold) en títulos, encabezados y etiquetas, y sin letter-spacing.
 * La fuente del sistema en pantallas de baja densidad dibuja las letras
 * redondas (c, o, s) finas y abiertas, lo que da sensación de texto
 * "separado"; un peso mayor rellena esos trazos y aprieta el texto.
 */
val Typography = Typography().let {
    it.copy(
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleSmall = it.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelLarge = it.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelMedium = it.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    )
}