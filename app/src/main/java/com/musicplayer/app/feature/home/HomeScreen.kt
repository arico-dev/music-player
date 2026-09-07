package com.musicplayer.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musicplayer.app.R
import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.usage.UsageData
import com.musicplayer.app.feature.common.AlbumCard
import java.time.LocalDateTime

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    isPlaying: Boolean = false,
    currentSongId: Long? = null,
    onResumeSession: () -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onRecentSongClick: (Song) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {}
) {
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()
    val resumeEntry by viewModel.resumeEntry.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val topAlbums by viewModel.topAlbums.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    val now = LocalDateTime.now()

    BoxWithConstraints(modifier = modifier) {
        val columns = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 620.dp -> 3
            else -> 2
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "greeting", span = { GridItemSpan(maxLineSpan) }) {
                Greeting()
            }
            if (usage.totalPlayedMs > 0L || usage.totalPlays > 0) {
                item(key = "stats", span = { GridItemSpan(maxLineSpan) }) {
                    StatsCard(usage = usage, now = now)
                }
            }
            val resume = resumeEntry
            if (resume != null) {
                val trackingThis = isPlaying && resume.song.id == currentSongId
                item(key = "resume", span = { GridItemSpan(maxLineSpan) }) {
                    ResumeCard(
                        song = resume.song,
                        nowPlaying = trackingThis,
                        onAction = if (trackingThis) onTogglePlayPause else onResumeSession
                    )
                }
            }
            if (topAlbums.isNotEmpty()) {
                item(key = "albums_title", span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(text = stringResource(R.string.home_top_albums))
                }
                items(topAlbums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                }
            }
            item(key = "recents_title", span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(text = stringResource(R.string.home_recents))
            }
            if (recentSongs.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    RecentsEmpty()
                }
            } else {
                recentSongs.forEach { song ->
                    item(key = "recent-${song.id}", span = { GridItemSpan(maxLineSpan) }) {
                        RecentRow(
                            song = song,
                            onClick = { onRecentSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting() {
    val now = LocalDateTime.now()
    val greeting = when (now.hour) {
        in 6 until 12 -> stringResource(R.string.home_greeting_morning)
        in 12 until 20 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_evening)
    }
    Text(
        text = greeting,
        style = MaterialTheme.typography.headlineSmall
    )
}

@Composable
private fun StatsCard(usage: UsageData, now: LocalDateTime) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_stats_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.size(8.dp))
            StatRow(label = stringResource(R.string.home_stats_last_hour), amount = formatDuration(usage.lastHourMs(now)))
            StatRow(label = stringResource(R.string.home_stats_today), amount = formatDuration(usage.todayMs(now)))
            StatRow(label = stringResource(R.string.home_stats_month), amount = formatDuration(usage.monthMs(now)))
            Spacer(modifier = Modifier.size(8.dp))
            val playsText = pluralStringResource(
                R.plurals.home_stat_replays, usage.totalPlays, usage.totalPlays
            )
            val songsText = pluralStringResource(
                R.plurals.home_stat_songs, usage.distinctSongs, usage.distinctSongs
            )
            Text(
                text = "$playsText en $songsText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatRow(label: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun ResumeCard(
    song: Song,
    nowPlaying: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onAction)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(song = song, size = 48.dp)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(if (nowPlaying) R.string.home_now_playing else R.string.home_resume),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onAction) {
            Icon(
                imageVector = if (nowPlaying) Icons.Filled.Pause else Icons.Filled.Replay,
                contentDescription = stringResource(
                    if (nowPlaying) R.string.home_now_playing_pause else R.string.home_resume_play
                )
            )
        }
    }
}

@Composable
private fun RecentRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(song = song, size = 44.dp)
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Artwork(song: Song, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
    }
}

@Composable
private fun RecentsEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.home_recents_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours} h ${minutes} min" else "${minutes} min"
}