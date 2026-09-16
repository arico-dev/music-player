package com.musicplayer.app.feature.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
            val transition = rememberInfiniteTransition(label = "nowPlaying")
            key("playing") {
                val p1 by transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 360, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar1"
                )
                val p2 by transition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 280, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar2"
                )
                val p3 by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 440, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar3"
                )
                EqualizerBar(progress = p1, color = color)
                EqualizerBar(progress = p2, color = color)
                EqualizerBar(progress = p3, color = color)
            }
        } else {
            key("paused") {
                EqualizerBar(progress = 0.45f, color = color)
                EqualizerBar(progress = 0.7f, color = color)
                EqualizerBar(progress = 0.5f, color = color)
            }
        }
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