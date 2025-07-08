package com.example.meta_glasses_prototype

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class FaceBlurWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val imageUris = MediaScanner.getImagesFromMetaAIFolder(context)
        var processedCount = 0

        imageUris.forEach { uri ->
            try {
                val fileName = getFileNameFromUri(context, uri)
                if (fileName != null && isImageAlreadyProcessed(context, "Processed_$fileName")) {
                    Log.d("Worker", "Skipping already processed: $fileName")
                    return@forEach
                }

                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()

                if (bitmap != null) {
                    val blurred = ImageProcessor.blurFaces(context, bitmap)
                    FileManager.saveBitmap(context, blurred, uri, prefix = "Processed_")
                    processedCount++
                }

            } catch (e: Exception) {
                Log.e("Worker", "Error processing image: ${e.message}")
            }
        }

        Log.d("Worker", "Done. Processed $processedCount image(s)")
        return Result.success()
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                return cursor.getString(idx)
            }
        }
        return null
    }

    private fun isImageAlreadyProcessed(context: Context, fileName: String): Boolean {
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
