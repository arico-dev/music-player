package com.musicplayer.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.musicplayer.app.R

/**
 * Nunito: fuente redonda y "cute" (inspirada en Chocola). Se empaqueta la variable
 * [Nunito[wght].ttf] declarada en res/font/nunito.xml para los pesos 200..800, así
 * que cada peso aplica la variación real (wght) en lugar de faux-bold.
 */
val NunitoFamily = FontFamily(
    Font(R.font.nunito, weight = FontWeight.ExtraLight),
    Font(R.font.nunito, weight = FontWeight.Normal),
    Font(R.font.nunito, weight = FontWeight.Medium),
    Font(R.font.nunito, weight = FontWeight.SemiBold),
    Font(R.font.nunito, weight = FontWeight.Bold),
    Font(R.font.nunito, weight = FontWeight.ExtraBold)
)

/**
 * Escala tipográfica redondeada al estilo Chocola: pesos generosos en títulos,
 * encabezados y etiquetas, sin letter-spacing para apretar el texto.
 */
private val Base = Typography()

val Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    displayMedium = Base.displayMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    displaySmall = Base.displaySmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    headlineLarge = Base.headlineLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    headlineMedium = Base.headlineMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    headlineSmall = Base.headlineSmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleLarge = Base.titleLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleMedium = Base.titleMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = Base.titleSmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyLarge = Base.bodyLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    bodyMedium = Base.bodyMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    labelLarge = Base.labelLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    labelMedium = Base.labelMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    labelSmall = Base.labelSmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
)