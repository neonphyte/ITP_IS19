package com.example.meta_glasses_prototype

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var helloText: TextView
    //private var folderObserver: MetaFolderObserver? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (arePermissionsFullyGranted()) {
            helloText.text = "Gallery access granted ✅\nWatching Meta AI folder..."
            //startFolderObserver()
            val serviceIntent = Intent(this, MetaFolderService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)

        } else if (Build.VERSION.SDK_INT >= 34) {
            showLimitedAccessDialog()
        } else {
            helloText.text = "Gallery access denied ❌"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        helloText = findViewById(R.id.helloText)
        val clickButton = findViewById<Button>(R.id.clickButton)

        clickButton.setOnClickListener {
            requestGalleryPermission()
        }
    }

    private fun requestGalleryPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissions)
    }

    private fun arePermissionsFullyGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showLimitedAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Limited Access Detected")
            .setMessage("You've granted limited photo access. To allow full gallery access, go to Settings.")
            .setPositiveButton("Open Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

//    private fun startFolderObserver() {
//        if (folderObserver == null) {
//            folderObserver = MetaFolderObserver(this)
//            folderObserver?.startWatching()
//        }
//
//        // Scan existing images
//        processExistingImages()
//    }

    override fun onDestroy() {
//        folderObserver?.stopWatching()
        super.onDestroy()
    }

    private fun processExistingImages() {
        lifecycleScope.launch {
            val imageUris = MediaScanner.getImagesFromMetaAIFolder(this@MainActivity)
            var processedCount = 0

            for (uri in imageUris) {
                try {
                    val fileName = getFileNameFromUri(uri) ?: continue
                    if (FileManager.isAlreadyProcessed(this@MainActivity, "Processed_$fileName")) continue

                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        val blurred = ImageProcessor.blurFaces(this@MainActivity, bitmap)
                        FileManager.saveBitmap(this@MainActivity, blurred, uri, "Processed_")
                        processedCount++
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            helloText.text = "Processed $processedCount existing image(s) ✅"
        }
    }


    private fun getFileNameFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            return if (cursor.moveToFirst()) cursor.getString(idx) else null
        }
        return null
    }
}
