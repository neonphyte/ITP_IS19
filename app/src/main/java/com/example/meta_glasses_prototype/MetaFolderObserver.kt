package com.example.meta_glasses_prototype

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.FileObserver
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MetaFolderObserver(private val context: Context) : FileObserver(getWatchPath()) {

    companion object {
        private const val TAG = "MetaFolderObserver"
        private const val META_FOLDER_PATH = "/storage/emulated/0/Download/Meta AI" // Absolute path

        fun getWatchPath(): String = META_FOLDER_PATH
    }

    override fun onEvent(event: Int, fileName: String?) {
        if (event == CREATE || event == MOVED_TO) {
            if (fileName != null && isImageFile(fileName)) {
                val fullPath = "$META_FOLDER_PATH/$fileName"
                Log.d(TAG, "Detected new image: $fileName")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Avoid reprocessing
                        val processedFileName = "Processed_$fileName"
                        if (isImageAlreadyProcessed(processedFileName)) {
                            Log.d(TAG, "Already processed: $processedFileName")
                            return@launch
                        }

                        val file = File(fullPath)
                        if (!file.exists()) return@launch

                        val bitmap = BitmapFactory.decodeFile(fullPath) ?: return@launch
                        val blurred = ImageProcessor.blurFaces(context, bitmap)

                        // Save with processed prefix
                        FileManager.saveBitmapFromFile(context, blurred, file, "Processed_")

                        Log.d(TAG, "Processed and saved: $processedFileName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing new image: ${e.message}")
                    }
                }
            }
        }
    }

    private fun isImageFile(name: String): Boolean {
        return name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true)
    }

    private fun isImageAlreadyProcessed(fileName: String): Boolean {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            return cursor.moveToFirst()
        }

        return false
    }
}
