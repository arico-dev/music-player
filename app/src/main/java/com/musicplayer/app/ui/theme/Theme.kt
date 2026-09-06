package com.musicplayer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = OffWhite,
    primaryContainer = PaleSky,
    onPrimaryContainer = JetBlack,
    secondary = JetBlack,
    onSecondary = OffWhite,
    secondaryContainer = Lavender,
    onSecondaryContainer = JetBlack,
    tertiary = Teal,
    onTertiary = OffWhite,
    tertiaryContainer = Lavender,
    onTertiaryContainer = JetBlack,
    background = OffWhite,
    onBackground = JetBlack,
    surface = OffWhite,
    onSurface = JetBlack,
    surfaceVariant = Lavender,
    onSurfaceVariant = LightSlate,
    surfaceContainerLowest = OffWhite,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    inverseSurface = JetBlack,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = PaleSky,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTeal,
    onPrimary = JetBlack,
    primaryContainer = Teal,
    onPrimaryContainer = OffWhite,
    secondary = DarkSecondary,
    onSecondary = JetBlack,
    secondaryContainer = DarkSurfaceContainer,
    onSecondaryContainer = Lavender,
    tertiary = PaleSky,
    onTertiary = JetBlack,
    tertiaryContainer = DarkSurfaceHigh,
    onTertiaryContainer = Lavender,
    background = DarkSurfaceLowest,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    inverseSurface = DarkOnSurface,
    inverseOnSurface = JetBlack,
    inversePrimary = Teal,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceHigh,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}