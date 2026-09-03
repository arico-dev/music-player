package com.musicplayer.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.palette.graphics.Palette
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumArtColorExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Devuelve el color dominante (fallback a null si no se puede derivar). */
    fun extractFromUri(uri: Uri?): Int? {
        if (uri == null) return null
        val bitmap = loadBitmap(uri) ?: return null
        return runCatching {
            val palette = Palette.from(bitmap).generate()
            palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
        }.getOrNull()
    }

    private fun loadBitmap(uri: Uri): Bitmap? = runCatching {
        context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
    }.getOrNull()
}
