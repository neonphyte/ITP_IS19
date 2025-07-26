package com.example.meta_glasses_prototype

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MetaFolderService : Service() {

    private lateinit var observer: FileObserver

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        processExistingImagesOnce() // ✅ Scan existing images
        startObservingMetaFolder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startObservingMetaFolder() {
        val metaFolderPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Meta AI"
        ).absolutePath

        observer = object : FileObserver(metaFolderPath, CREATE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null) {
                    val fullPath = "$metaFolderPath/$path"
                    Log.d("MetaFolderService", "📥 New file detected: $fullPath")

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val file = File(fullPath)
                            if (!file.exists()) return@launch

                            if (file.name.startsWith("Processed_")) return@launch
                            if (FileManager.isAlreadyProcessed(applicationContext, "Processed_${file.name}")) return@launch

                            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@launch
                            val blurred = ImageProcessor.blurFaces(applicationContext, bitmap)
                            FileManager.saveBitmapFromFile(applicationContext, blurred, file, "Processed_")

                            Log.d("MetaFolderService", "✅ Processed and saved: ${file.name}")
                        } catch (e: Exception) {
                            Log.e("MetaFolderService", "❌ Error: ${e.message}")
                        }
                    }
                }
            }
        }

        observer.startWatching()
    }

    private fun processExistingImagesOnce() {
        CoroutineScope(Dispatchers.IO).launch {
            val metaFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Meta AI")
            if (!metaFolder.exists() || !metaFolder.isDirectory) return@launch

            val imageFiles = metaFolder.listFiles { file ->
                file.isFile && !file.name.startsWith("Processed_")
            } ?: return@launch

            for (file in imageFiles) {
                try {
                    if (FileManager.isAlreadyProcessed(applicationContext, "Processed_${file.name}")) continue

                    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                    val blurred = ImageProcessor.blurFaces(applicationContext, bitmap)
                    FileManager.saveBitmapFromFile(applicationContext, blurred, file, "Processed_")

                    Log.d("MetaFolderService", "🧠 Processed existing file: ${file.name}")
                } catch (e: Exception) {
                    Log.e("MetaFolderService", "❌ Error on existing file: ${e.message}")
                }
            }
        }
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "meta_service_channel"
        val channelName = "Meta AI Folder Monitor"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Meta AI Monitor Running")
            .setContentText("Monitoring Meta AI folder for new images")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        observer.stopWatching()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
