package com.musicplayer.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.musicplayer.app.R

/**
 * Colores semilla "cute" disponibles en Ajustes → Apariencia.
 * Cualquier color ARGB es válido como semilla; estos son los presets.
 */
data class SeedPreset(@StringRes val nameRes: Int, val color: Color)

val SeedPresets = listOf(
    SeedPreset(R.string.settings_seed_rosa, Color(0xFFFF5597)),
    SeedPreset(R.string.settings_seed_frambuesa, Color(0xFFC2185B)),
    SeedPreset(R.string.settings_seed_naranja, Color(0xFFFF7A45)),
    SeedPreset(R.string.settings_seed_ambar, Color(0xFFFFB300)),
    SeedPreset(R.string.settings_seed_lima, Color(0xFF9CCC65)),
    SeedPreset(R.string.settings_seed_verde, Color(0xFF26A69A)),
    SeedPreset(R.string.settings_seed_cielo, Color(0xFF29B6F6)),
    SeedPreset(R.string.settings_seed_azul, Color(0xFF5C6BC0)),
    SeedPreset(R.string.settings_seed_lila, Color(0xFFAB47BC)),
    SeedPreset(R.string.settings_seed_violeta, Color(0xFF7E57C2))
)