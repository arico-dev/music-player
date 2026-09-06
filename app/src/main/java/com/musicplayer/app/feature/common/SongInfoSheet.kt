package com.musicplayer.app.feature.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.app.R
import com.musicplayer.app.core.model.SongMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    song: SongMetadata,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
        ) {
            Text(
                text = stringResource(R.string.song_info_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(stringResource(R.string.song_info_title_label), song.title)
            InfoRow(stringResource(R.string.song_info_artist_label), song.artist)
            InfoRow(stringResource(R.string.song_info_album_label), song.album)
            song.genre?.takeIf { it.isNotBlank() }?.let { InfoRow(stringResource(R.string.song_info_genre_label), it) }
            song.trackNumber?.let { InfoRow(stringResource(R.string.song_info_track_label), it.toString()) }
            InfoRow(stringResource(R.string.song_info_duration_label), formatDuration(song.durationMs))
            song.fileSizeBytes?.let { InfoRow(stringResource(R.string.song_info_size_label), formatFileSize(it)) }
            song.mimeType?.takeIf { it.isNotBlank() }?.let { InfoRow(stringResource(R.string.song_info_type_label), it) }
            song.codec?.takeIf { it.isNotBlank() }?.let { InfoRow(stringResource(R.string.song_info_codec_label), it) }
            song.bitrateBps?.let { InfoRow(stringResource(R.string.song_info_bitrate_label), formatBitrate(it)) }
            song.sampleRateHz?.let { InfoRow(stringResource(R.string.song_info_sample_rate_label), "$it Hz") }
            InfoRow(stringResource(R.string.song_info_path_label), song.path)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

private fun formatBitrate(bps: Long): String = when {
    bps >= 1_000_000 -> "%.1f Mbps".format(bps / 1_000_000.0)
    bps >= 1_000 -> "${bps / 1_000} kbps"
    else -> "$bps bps"
}