package com.musicplayer.app.feature.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.musicplayer.app.MainActivity
import com.musicplayer.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Widget de pantalla de inicio con controles de reproducción.
 *
 * La composición OBSERVA [WidgetStateStore.flow] (recomposición = estado fresco siempre);
 * por eso `update()`/`recompose` reflejan el estado actual aunque `provideGlance` no se
 * vuelva a entrar (Glance no reejecuta `provideGlance` si la sesión ya está activa).
 * Los botones lanzan broadcasts a [MusicWidgetActionReceiver].
 */
object MusicWidget : GlanceAppWidget() {

    const val EXTRA_ACTION = "com.musicplayer.app.widget.action"
    const val ACTION_TOGGLE = "toggle"
    const val ACTION_NEXT = "next"
    const val ACTION_PREVIOUS = "previous"

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialState = WidgetStateStore.load(context)
        // Instancia estable del estado observado durante toda la sesión de composición.
        val stateFlow = WidgetStateStore.flow(context)
        val actions = WidgetActions(
            openApp = actionStartActivity<MainActivity>(),
            previous = broadcast(context, ACTION_PREVIOUS),
            toggle = broadcast(context, ACTION_TOGGLE),
            next = broadcast(context, ACTION_NEXT),
        )
        val emptyLabel = context.getString(R.string.no_track_selected)
        val playLabel = context.getString(R.string.play)
        val pauseLabel = context.getString(R.string.pause)
        val previousLabel = context.getString(R.string.previous)
        val nextLabel = context.getString(R.string.next)
        provideContent {
            MusicWidgetContent(
                initialState = initialState,
                stateFlow = stateFlow,
                actions = actions,
                emptyLabel = emptyLabel,
                playLabel = playLabel,
                pauseLabel = pauseLabel,
                previousLabel = previousLabel,
                nextLabel = nextLabel,
            )
        }
    }

    private fun broadcast(context: Context, action: String): Action =
        actionSendBroadcast(
            Intent(context, MusicWidgetActionReceiver::class.java).putExtra(EXTRA_ACTION, action)
        )

    private const val TAG = "MusicWidget"
}

data class WidgetActions(
    val openApp: Action,
    val previous: Action,
    val toggle: Action,
    val next: Action,
)

private val WidgetBg = ColorProvider(Color(0xFF1C1B1F))
private val WidgetOnSurface = ColorProvider(Color(0xFFE6E0E9))
private val WidgetOnSurfaceVariant = ColorProvider(Color(0xFFBDB7C2))
private val WidgetBtnBg = ColorProvider(Color(0x33D0BCFF))

@Composable
fun MusicWidgetContent(
    initialState: WidgetState,
    stateFlow: Flow<WidgetState>,
    actions: WidgetActions,
    emptyLabel: String,
    playLabel: String,
    pauseLabel: String,
    previousLabel: String,
    nextLabel: String,
) {
    val state by stateFlow.collectAsState(initial = initialState)
    val artwork by rememberCoverArtwork(state)
    // Fondo = el propio cover oscurecido (como en el player); sin cover, fondo oscuro fijo.
    val rootModifier = if (artwork.isCover) {
        GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .appWidgetBackground()
            .background(artwork.provider, alpha = 0.5f)
            .cornerRadius(24.dp)
            .clickable(actions.openApp)
    } else {
        GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .appWidgetBackground()
            .background(WidgetBg)
            .cornerRadius(24.dp)
            .clickable(actions.openApp)
    }
    Column(
        modifier = rootModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = artwork.provider,
                contentDescription = null,
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(8.dp),
            )
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = state.title ?: emptyLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = WidgetOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                if (state.hasMedia) {
                    Text(
                        text = state.artist.orEmpty(),
                        maxLines = 1,
                        style = TextStyle(
                            color = WidgetOnSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SquareIconButton(
                imageProvider = ImageProvider(R.drawable.ic_widget_skip_previous),
                contentDescription = previousLabel,
                onClick = actions.previous,
                backgroundColor = WidgetBtnBg,
                contentColor = WidgetOnSurface,
            )
            Spacer(GlanceModifier.width(4.dp))
            SquareIconButton(
                imageProvider = if (state.isPlaying) {
                    ImageProvider(R.drawable.ic_widget_pause)
                } else {
                    ImageProvider(R.drawable.ic_widget_play)
                },
                contentDescription = if (state.isPlaying) pauseLabel else playLabel,
                onClick = actions.toggle,
                backgroundColor = WidgetBtnBg,
                contentColor = WidgetOnSurface,
            )
            Spacer(GlanceModifier.width(4.dp))
            SquareIconButton(
                imageProvider = ImageProvider(R.drawable.ic_widget_skip_next),
                contentDescription = nextLabel,
                onClick = actions.next,
                backgroundColor = WidgetBtnBg,
                contentColor = WidgetOnSurface,
            )
        }
    }
}

/** Carátula de fondo + miniatura; [isCover] indica si hay arte real (no el fallback). */
private class CoverArtwork(val provider: ImageProvider, val isCover: Boolean)

/** Decodifica la carátula una vez y la reutiliza como miniatura y como fondo del widget. */
@Composable
private fun rememberCoverArtwork(state: WidgetState): State<CoverArtwork> {
    val context = LocalContext.current
    val fallback = ImageProvider(R.drawable.ic_widget_music_note)
    val coverUri = state.albumArtUri
        ?: state.albumId?.let(::buildAlbumArtUri)
    return produceState(initialValue = CoverArtwork(fallback, false), coverUri, state.title) {
        value = if (coverUri == null || !state.hasMedia) {
            CoverArtwork(fallback, false)
        } else {
            val bitmap = withContext(Dispatchers.IO) { decodeCoverBitmap(context, coverUri) }
            if (bitmap != null) CoverArtwork(ImageProvider(bitmap), true)
            else CoverArtwork(fallback, false)
        }
    }
}

private fun decodeCoverBitmap(context: Context, uri: Uri): Bitmap? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()

/** `content://media/external/audio/albumart/<id>` (requiere solo READ_MEDIA_AUDIO). */
private fun buildAlbumArtUri(albumId: Long): Uri =
    Uri.parse("content://media/external/audio/albumart").buildUpon()
        .appendPath(albumId.toString())
        .build()