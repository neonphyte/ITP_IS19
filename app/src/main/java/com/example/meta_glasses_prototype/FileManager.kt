package com.example.meta_glasses_prototype

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.IOException
import java.io.OutputStream

object FileManager {

    /**
     * Save a bitmap to a separate folder with a "Processed_" prefix added to the original filename.
     * Works on Android Q+ (scoped storage) and below.
     *
     * @param context - application context
     * @param bitmap - processed bitmap to save
     * @param originalUri - URI of the original image (to get filename)
     * @param prefix - prefix to add to saved filename, e.g., "Processed_"
     */
    fun saveBitmap(context: Context, bitmap: Bitmap, originalUri: Uri, prefix: String = "Processed_") {
        val resolver = context.contentResolver

        // Try to get the original filename from the URI
        val originalName = queryDisplayName(resolver, originalUri) ?: "image_${System.currentTimeMillis()}.jpg"
        val fileName = prefix + originalName

        val imageCollection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ProcessedImages") // Your folder
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(imageCollection, contentValues)

        if (imageUri != null) {
            var stream: OutputStream? = null
            try {
                stream = resolver.openOutputStream(imageUri)
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                stream?.close()
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
        }
    }

    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return null
    }
}
