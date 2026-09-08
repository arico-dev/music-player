package com.musicplayer.app.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.media3.common.Player
import androidx.compose.material3.Text
import androidx.compose.ui.unit.DpSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.musicplayer.app.R
import com.musicplayer.app.core.model.LrcLine
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.feature.common.SongInfoSheet

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val isShuffled by viewModel.isShuffled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val dominantColor by viewModel.dominantColor.collectAsStateWithLifecycle()
    val songInfo by viewModel.songInfo.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    BackHandler(enabled = showQueue || showLyrics) {
        if (showQueue) showQueue = false else if (showLyrics) showLyrics = false
    }
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showQueue -> QueueSheet(
                queue = queue,
                currentIndex = currentIndex,
                dominantColor = dominantColor,
                onClose = { showQueue = false },
                onSelect = viewModel::skipToIndex,
                onInfo = viewModel::requestSongInfo
            )
            showLyrics -> LyricsSheet(
                lyrics = lyrics,
                position = position,
                duration = duration,
                dominantColor = dominantColor,
                onClose = { showLyrics = false },
                onSeek = viewModel::seekTo,
                onRetry = viewModel::retryLyrics
            )
            else -> PlayerContent(
                currentSong = currentSong,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                isShuffled = isShuffled,
                repeatMode = repeatMode,
                dominantColor = dominantColor,
                queueSize = queue.size,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeatMode,
                onTogglePlayPause = viewModel::togglePlayPause,
                onPrevious = viewModel::skipToPrevious,
                onNext = viewModel::skipToNext,
                onSeek = viewModel::seekTo,
                onOpenQueue = { showQueue = true },
                onOpenLyrics = { showLyrics = true }
            )
        }
        songInfo?.let { song ->
            SongInfoSheet(
                song = song,
                onDismiss = viewModel::dismissSongInfo
            )
        }
    }
}

@Composable
private fun PlayerContent(
    currentSong: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isShuffled: Boolean,
    repeatMode: Int,
    dominantColor: Int?,
    queueSize: Int,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    val baseColor = dominantColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
    val onBase = if (dominantColor != null) Color.White else MaterialTheme.colorScheme.onSurface
    val backgroundColors = if (dominantColor != null) {
        val tint = baseColor.darken(0.45f)
        listOf(tint, tint.copy(alpha = 0.55f), Color.Black)
    } else {
        listOf(baseColor, baseColor.copy(alpha = 0.55f), Color.Black)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = backgroundColors))
    ) {
        if (maxWidth > maxHeight) {
            // Horizontal: carátula a la izquierda, controles a la derecha
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(
                    artUri = currentSong?.albumArtUri?.toString(),
                    onBase = onBase,
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .aspectRatio(1f)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SongTitleBlock(currentSong = currentSong, onBase = onBase)
                    Spacer(modifier = Modifier.height(24.dp))
                    SeekRow(
                        position = position,
                        duration = duration,
                        onSeek = onSeek,
                        onBase = onBase
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PlayerControlsRow(
                        isPlaying = isPlaying,
                        isShuffled = isShuffled,
                        repeatMode = repeatMode,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onTogglePlayPause = onTogglePlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onBase = onBase
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionChipsRow(
                        queueSize = queueSize,
                        onOpenQueue = onOpenQueue,
                        onOpenLyrics = onOpenLyrics,
                        onBase = onBase
                    )
                }
            }
        } else {
            // Vertical: columna centrada
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AlbumArt(
                    artUri = currentSong?.albumArtUri?.toString(),
                    onBase = onBase,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(1f)
                )
                Spacer(modifier = Modifier.height(40.dp))
                SongTitleBlock(currentSong = currentSong, onBase = onBase)
                Spacer(modifier = Modifier.height(32.dp))
                SeekRow(
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    onBase = onBase
                )
                Spacer(modifier = Modifier.height(16.dp))
                PlayerControlsRow(
                    isPlaying = isPlaying,
                    isShuffled = isShuffled,
                    repeatMode = repeatMode,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onTogglePlayPause = onTogglePlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onBase = onBase
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionChipsRow(
                    queueSize = queueSize,
                    onOpenQueue = onOpenQueue,
                    onOpenLyrics = onOpenLyrics,
                    onBase = onBase
                )
            }
        }
    }
}

@Composable
private fun SongTitleBlock(
    currentSong: Song?,
    onBase: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.animateContentSize()
    ) {
        AnimatedContent(targetState = currentSong?.title ?: "Nada sonando", label = "title") { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = onBase,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        AnimatedContent(targetState = currentSong?.artist ?: "", label = "artist") { artist ->
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = onBase.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SeekRow(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onBase: Color
) {
    if (duration > 0) {
        var dragValue by remember { mutableStateOf<Float?>(null) }
        var isDragging by remember { mutableStateOf(false) }
        val interactionSource = remember { MutableInteractionSource() }
        val thumbSize by animateDpAsState(
            targetValue = if (isDragging) 26.dp else 18.dp,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "seekThumb"
        )
        val sliderValue = dragValue ?: position.toFloat().coerceIn(0f, duration.toFloat())
        val timeLabel = (dragValue?.toLong() ?: position)
        Slider(
            value = sliderValue,
            onValueChange = {
                dragValue = it
                isDragging = true
            },
            onValueChangeFinished = {
                dragValue?.let { onSeek(it.toLong()) }
                dragValue = null
                isDragging = false
            },
            valueRange = 0f..duration.toFloat(),
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = onBase,
                activeTrackColor = onBase,
                inactiveTrackColor = onBase.copy(alpha = 0.3f)
            ),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(thumbColor = onBase),
                    thumbSize = DpSize(thumbSize, thumbSize)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = onBase,
                        inactiveTrackColor = onBase.copy(alpha = 0.3f)
                    ),
                    thumbTrackGapSize = 0.dp
                )
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(timeLabel),
                style = MaterialTheme.typography.labelMedium,
                color = onBase.copy(alpha = 0.8f)
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = onBase.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PlayerControlsRow(
    isPlaying: Boolean,
    isShuffled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBase: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = if (isShuffled) "Aleatorio activado" else "Aleatorio",
                tint = if (isShuffled) Color.White else onBase.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Anterior",
                tint = onBase,
                modifier = Modifier.size(40.dp)
            )
        }
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(onBase)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.Black,
                modifier = Modifier.size(48.dp)
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Siguiente",
                tint = onBase,
                modifier = Modifier.size(40.dp)
            )
        }
        IconButton(onClick = onCycleRepeat, modifier = Modifier.size(48.dp)) {
            val repeatDesc = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "Repetir una canción"
                Player.REPEAT_MODE_ALL -> "Repetir todo"
                else -> "Repetir"
            }
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = repeatDesc,
                tint = if (repeatMode != 0) Color.White else onBase.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ActionChipsRow(
    queueSize: Int,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onBase: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionChip(
            icon = Icons.Filled.QueueMusic,
            text = if (queueSize > 0) "Cola ($queueSize)" else "Cola",
            onClick = onOpenQueue,
            onBase = onBase
        )
        Spacer(modifier = Modifier.size(12.dp))
        ActionChip(
            icon = Icons.Filled.Lyrics,
            text = stringResource(R.string.lyrics),
            onClick = onOpenLyrics,
            onBase = onBase
        )
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    onBase: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .height(36.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = onBase.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = onBase.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AlbumArt(
    artUri: String?,
    onBase: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(onBase.copy(alpha = 0.15f))
    ) {
        Crossfade(targetState = artUri, label = "albumArt") { uri ->
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = stringResource(R.string.a11y_album_art),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = stringResource(R.string.a11y_album_art),
                    tint = onBase.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                )
            }
        }
    }
}

@Composable
private fun QueueSheet(
    queue: List<Song>,
    currentIndex: Int,
    dominantColor: Int?,
    onClose: () -> Unit,
    onSelect: (Int) -> Unit,
    onInfo: (Song) -> Unit
) {
    val baseColor = dominantColor?.let { Color(it) }
    val onBase = baseColor?.let { if (it.luminance() > 0.5f) Color.Black else Color.White }
    val closeColor = onBase ?: MaterialTheme.colorScheme.onSurface
    val titleColor = onBase ?: MaterialTheme.colorScheme.onSurface
    val currentHighlight = onBase?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceVariant
    val rowBackground = if (onBase != null) Color.Transparent else MaterialTheme.colorScheme.background
    val currentTitle = onBase ?: MaterialTheme.colorScheme.primary
    val defaultTitle = onBase?.copy(alpha = 0.92f) ?: MaterialTheme.colorScheme.onSurface
    val artistColor = onBase?.copy(alpha = 0.7f) ?: MaterialTheme.colorScheme.onSurfaceVariant
    val moreColor = onBase?.copy(alpha = 0.8f) ?: MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (baseColor != null) {
                    Brush.verticalGradient(
                        listOf(baseColor, baseColor.copy(alpha = 0.45f), Color.Black)
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                }
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Cerrar cola",
                tint = closeColor,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .size(32.dp)
            )
            Text(
                text = "Cola",
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                val isCurrent = index == currentIndex
                var menuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .background(if (isCurrent) currentHighlight else rowBackground)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (song.albumArtUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(song.albumArtUri),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = if (isCurrent) currentTitle else moreColor,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isCurrent) currentTitle else defaultTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = artistColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = moreColor
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.song_info_title)) },
                                onClick = {
                                    menuExpanded = false
                                    onInfo(song)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsSheet(
    lyrics: LyricsUiState,
    position: Long,
    duration: Long,
    dominantColor: Int?,
    onClose: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit
) {
    val baseColor = dominantColor?.let { Color(it) }
    val foreground = if (baseColor != null) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (baseColor != null) {
                    Brush.verticalGradient(
                        listOf(
                            baseColor.darken(0.5f),
                            baseColor.darken(0.72f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Cerrar letras",
                    tint = foreground
                )
            }
            Text(
                text = stringResource(R.string.lyrics),
                style = MaterialTheme.typography.titleMedium,
                color = foreground
            )
        }
        when (lyrics) {
            LyricsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = foreground)
            }
            LyricsUiState.Instrumental -> LyricsCenteredMessage(
                text = stringResource(R.string.lyrics_instrumental),
                textColor = foreground,
                onRetry = null
            )
            LyricsUiState.NotFound -> LyricsCenteredMessage(
                text = stringResource(R.string.lyrics_not_found),
                textColor = foreground,
                onRetry = onRetry
            )
            is LyricsUiState.Plain -> PlainLyrics(
                text = lyrics.text,
                position = position,
                duration = duration,
                accent = baseColor,
                inactiveColor = foreground.copy(alpha = 0.62f)
            )
            is LyricsUiState.Synced -> SyncedLyrics(
                lines = lyrics.lines,
                position = position,
                onSeek = onSeek,
                accent = baseColor,
                inactiveColor = foreground.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun LyricsCenteredMessage(
    text: String,
    textColor: Color,
    onRetry: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor.copy(alpha = if (textColor == Color.White) 0.75f else 1f),
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.lyrics_retry))
            }
        }
    }
}

@Composable
private fun PlainLyrics(
    text: String,
    position: Long,
    duration: Long,
    accent: Color?,
    inactiveColor: Color
) {
    val activeBackground = if (accent != null) {
        if (accent.luminance() < 0.25f) accent.lighten(0.45f) else accent.lighten(0.12f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val activeText = if (activeBackground.luminance() > 0.5f) Color.Black else Color.White

    val lines = remember(text) { text.split('\n').filterNot { it.isBlank() } }
    if (lines.size <= 1) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = inactiveColor.copy(alpha = 1f)
            )
        }
        return
    }

    val perLineMs = if (duration > 0) duration / lines.size else PLAIN_LINE_FALLBACK_MS
    val rawIndex = (position / perLineMs).toInt()
    val currentIndex = rawIndex.coerceIn(0, lines.lastIndex)

    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, lines) {
        if (currentIndex > 0) {
            listState.animateScrollToItem(maxOf(0, currentIndex - 1))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            val active = index == currentIndex
            Text(
                text = line,
                style = if (active) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (active) activeText else inactiveColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) activeBackground else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun SyncedLyrics(
    lines: List<LrcLine>,
    position: Long,
    onSeek: (Long) -> Unit,
    accent: Color?,
    inactiveColor: Color
) {
    val activeBackground = if (accent != null) {
        if (accent.luminance() < 0.25f) accent.lighten(0.45f) else accent.lighten(0.12f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val activeText = if (activeBackground.luminance() > 0.5f) Color.Black else Color.White

    val listState = rememberLazyListState()
    var currentIndex = lines.indexOfLast { it.timeMs <= position }
    if (currentIndex < 0) currentIndex = 0

    LaunchedEffect(currentIndex, lines) {
        if (currentIndex > 0) {
            listState.animateScrollToItem(maxOf(0, currentIndex - 1))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            val active = index == currentIndex
            Text(
                text = line.text,
                style = if (active) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (active) activeText else inactiveColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (active) activeBackground else Color.Transparent
                    )
                    .clickable { onSeek(line.timeMs) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

private fun Color.darken(amount: Float): Color = lerp(this, Color.Black, amount.coerceIn(0f, 1f))

private fun Color.lighten(amount: Float): Color = lerp(this, Color.White, amount.coerceIn(0f, 1f))

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private const val PLAIN_LINE_FALLBACK_MS = 4000L
