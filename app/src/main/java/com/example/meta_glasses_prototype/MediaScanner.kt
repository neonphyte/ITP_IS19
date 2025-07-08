package com.example.meta_glasses_prototype

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

object MediaScanner {

    private const val TAG = "MediaScanner"
    private const val META_FOLDER_NAME = "Download/Meta AI"

    fun getImagesFromMetaAIFolder(context: Context): List<Uri> {
        val imageUris = mutableListOf<Uri>()

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use RELATIVE_PATH
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("%$META_FOLDER_NAME%")
        } else {
            // Legacy fallback — check full DATA path
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            selectionArgs = arrayOf("%/$META_FOLDER_NAME/%")
        }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                imageUris.add(uri)
            }
        } ?: Log.w(TAG, "No images found in folder: $META_FOLDER_NAME")

        return imageUris
    }
}
