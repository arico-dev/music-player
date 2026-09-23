package com.musicplayer.app.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Cebolla que simula un ecualizador vivo pero sin animar a 60 fps: la fase avanza
 * en pasos discretos (~8 pasos/s) y se lee dentro de [androidx.compose.ui.graphics.drawscope.DrawScope],
 * de modo que cada paso solo invalida el dibujo de las barras (nada de recomposición
 * ni animación por frame).
 */
@Composable
fun NowPlayingIndicator(
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.width(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            val phaseState = remember { mutableIntStateOf(0) }
            LaunchedEffect(phaseState) {
                while (isActive) {
                    delay(EQUALIZER_STEP_MS)
                    phaseState.intValue = phaseState.intValue + 1
                }
            }
            EqualizerBar(phaseState = phaseState, phaseOffset = 0, color = color)
            EqualizerBar(phaseState = phaseState, phaseOffset = 3, color = color)
            EqualizerBar(phaseState = phaseState, phaseOffset = 5, color = color)
        } else {
            EqualizerBar(progress = 0.45f, color = color)
            EqualizerBar(progress = 0.7f, color = color)
            EqualizerBar(progress = 0.5f, color = color)
        }
    }
}

/** Máximo de fase antes de que la barra repita su patrón. */
private const val EQUALIZER_CYCLE = 8

/** Milisegundos entre cada paso de la animación de las barras (imita ~8 fps). */
private const val EQUALIZER_STEP_MS = 120L

/**
 * Progreso de una barra para una fase y un desfase dados. Genera un patrón
 * determinista "waweado" sin depender de rng ni de animaciones por frame.
 */
private fun equalizerValue(phase: Int, offset: Int): Float {
    val step = (phase + offset) % EQUALIZER_CYCLE
    return when (step) {
        0 -> 0.35f
        1 -> 0.55f
        2 -> 0.9f
        3 -> 1f
        4 -> 0.8f
        5 -> 0.45f
        6 -> 0.7f
        7 -> 0.55f
        else -> 0.5f
    }
}

@Composable
private fun EqualizerBar(
    progress: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(20.dp)
            .drawBehind {
                val barHeight = size.height * progress.coerceIn(0f, 1f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, size.height - barHeight),
                    size = Size(size.width, barHeight),
                    cornerRadius = CornerRadius(size.width / 2f)
                )
            }
    )
}

/**
 * Barra animada que avanza la [phaseState] en pasos de [phaseOffset]. La fase se lee
 * dentro del dibujo de la barra: cuando cambia solo se invalidan estos 3 draw nodes,
 * sin recomponer el árbol ni redibujar el resto de la pantalla.
 */
@Composable
private fun EqualizerBar(
    phaseState: MutableIntState,
    phaseOffset: Int,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(20.dp)
            .drawBehind {
                val progress = equalizerValue(phaseState.intValue, phaseOffset)
                val barHeight = size.height * progress.coerceIn(0f, 1f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, size.height - barHeight),
                    size = Size(size.width, barHeight),
                    cornerRadius = CornerRadius(size.width / 2f)
                )
            }
    )
}