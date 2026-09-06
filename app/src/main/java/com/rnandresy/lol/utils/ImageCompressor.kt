package com.rnandresy.lol.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Compresse une image avant upload.
 * Sur un réseau mobile malgache, ça divise le poids par 5 à 10
 * sans différence visible à l'écran.
 */
object ImageCompressor {

    data class Result(
        val bytes: ByteArray,
        val originalSizeKb: Int,
        val compressedSizeKb: Int
    ) {
        val savedPercent: Int get() =
            if (originalSizeKb == 0) 0
            else ((originalSizeKb - compressedSizeKb) * 100 / originalSizeKb)
                .coerceIn(0, 100)

        override fun equals(other: Any?): Boolean =
            other is Result && bytes.contentEquals(other.bytes)
        override fun hashCode(): Int = bytes.contentHashCode()
    }

    suspend fun compress(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_IMAGE_DIMENSION,
        quality: Int      = IMAGE_QUALITY
    ): Result = withContext(Dispatchers.IO) {

        val originalBytes = context.contentResolver
            .openInputStream(uri)?.use { it.readBytes() }
            ?: error("Impossible de lire l'image")

        val originalKb = originalBytes.size / 1024

        // 1) Lecture des dimensions sans charger le bitmap complet
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, bounds)

        // 2) Calcul du facteur de sous-échantillonnage (puissance de 2)
        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var bitmap = BitmapFactory
            .decodeByteArray(originalBytes, 0, originalBytes.size, opts)
            ?: error("Format d'image non supporté")

        // 3) Redimensionnement fin si encore trop grand
        bitmap = scaleToMax(bitmap, maxDimension)

        // 4) Correction de la rotation EXIF (photos prises à l'horizontale)
        bitmap = applyExifRotation(context, uri, bitmap)

        // 5) Compression JPEG
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bitmap.recycle()

        val compressed = out.toByteArray()
        out.close()

        // Si la compression a grossi le fichier (rare, PNG déjà optimisé), on garde l'original
        val final = if (compressed.size < originalBytes.size) compressed else originalBytes

        Result(
            bytes            = final,
            originalSizeKb   = originalKb,
            compressedSizeKb = final.size / 1024
        )
    }

    private fun calculateInSampleSize(w: Int, h: Int, max: Int): Int {
        var sample = 1
        var width  = w
        var height = h
        while (width / 2 >= max && height / 2 >= max) {
            width  /= 2
            height /= 2
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun scaleToMax(src: Bitmap, max: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= max && h <= max) return src

        val ratio  = if (w > h) max.toFloat() / w else max.toFloat() / h
        val newW   = (w * ratio).toInt().coerceAtLeast(1)
        val newH   = (h * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        if (scaled != src) src.recycle()
        return scaled
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap =
        runCatching {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            // `return bitmap` était refusé ici : la fonction a un corps
            // d'expression, et Kotlin n'y autorise pas de retour non local.
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) return@runCatching bitmap

            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap) bitmap.recycle()
            rotated
        }.getOrElse { bitmap }
}