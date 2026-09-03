package com.haruchi.today.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies photos picked from the system photo picker into app-private storage.
 *
 * We never keep the content Uri (its grant is transient); instead we decode, down-scale,
 * and write a JPEG we own. This is the "permission 0" storage story from the PRD: the app
 * holds only files it created, under [Context.getFilesDir]/photos.
 */
@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File by lazy {
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }
    }

    fun fileFor(fileName: String): File = File(dir, fileName)

    /**
     * Imports [uri] into private storage.
     * @return the [PhotoRef] (file name + measured aspect) or null if decoding failed.
     */
    suspend fun import(uri: Uri, toneIndex: Int = 0): PhotoRef? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(uri) ?: return@runCatching null
            val name = "${UUID.randomUUID()}.jpg"
            FileOutputStream(fileFor(name)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            val aspect = if (bitmap.height == 0) 1f else bitmap.width.toFloat() / bitmap.height
            bitmap.recycle()
            PhotoRef(fileName = name, aspect = aspect, toneIndex = toneIndex)
        }.getOrNull()
    }

    fun delete(fileName: String) {
        runCatching { fileFor(fileName).delete() }
    }

    /**
     * Duplicates an already-stored photo under a new file name, for "reuse a recent photo"
     * pickers. Sessions each own their photo files (deleting a receipt deletes its files), so
     * reusing one directly by name would risk one receipt's delete wiping another's photo.
     */
    suspend fun copy(sourceFileName: String, aspect: Float, toneIndex: Int): PhotoRef? =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = fileFor(sourceFileName)
                if (!source.exists()) return@runCatching null
                val name = "${UUID.randomUUID()}.jpg"
                source.copyTo(fileFor(name), overwrite = true)
                PhotoRef(fileName = name, aspect = aspect, toneIndex = toneIndex)
            }.getOrNull()
        }

    /** Decodes a bitmap capped at [MAX_EDGE] px on its long side, honoring EXIF rotation. */
    private fun decodeScaled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)

        var sample = 1
        while (longest / sample > MAX_EDGE) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        return applyOrientation(decoded, orientation)
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    companion object {
        const val DIR_NAME = "photos"
        private const val MAX_EDGE = 2048

        /** Resolves a stored photo file from any context (used by the UI layer). */
        fun fileIn(context: Context, fileName: String): File =
            File(File(context.filesDir, DIR_NAME), fileName)
    }
}
