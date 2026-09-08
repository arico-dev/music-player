package com.musicplayer.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumArtColorExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val cache = LruCache<String, Int>(64)

    /** Devuelve un color ya cacheador para la portada, o null si aún no se ha calculado. */
    fun peekFromUri(uri: Uri?): Int? {
        if (uri == null) return null
        return cache.get(uri.toString())
    }

    /** Devuelve el color dominante (fallback a null si no se puede derivar), fuera del hilo principal y con caché por portada. */
    suspend fun extractFromUri(uri: Uri?): Int? {
        if (uri == null) return null
        val key = uri.toString()
        cache.get(key)?.let {
            Log.d(TAG, "color desde caché ($key)")
            return it
        }
        return withContext(Dispatchers.IO) {
            val color = decodeSampledBitmap(uri)?.let { bitmap ->
                runCatching {
                    val palette = Palette.from(bitmap).generate()
                    palette.vibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                }.getOrNull()
            }
            if (color != null) cache.put(key, color)
            color
        }
    }

    /** Decodifica la portada con downsampling a ~150px: Palette no necesita la imagen completa. */
    private fun decodeSampledBitmap(uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }.getOrNull()

    private fun computeInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > maxDimension) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val MAX_DIMENSION = 150
        const val TAG = "AlbumArtColor"
    }
}
