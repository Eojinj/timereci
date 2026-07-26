package com.timereci.focus.ui.detail

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves an exported card image to the shared Pictures collection via MediaStore and returns
 * its content Uri — already shareable cross-app (MediaStore-owned Uris don't need a
 * FileProvider), so the same save backs both "Save Image" and the system share sheet
 * (Merci v5's Share action replaces the old save-to-gallery button).
 * Uses scoped storage (RELATIVE_PATH) on API 29+ — no storage permission required, in
 * keeping with the app's "permission 0" stance.
 */
object Exporter {

    private const val ALBUM = "Merci"

    suspend fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$ALBUM")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null

            runCatching {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: return@withContext null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            }.getOrElse {
                resolver.delete(uri, null, null)
                null
            }
        }
}
