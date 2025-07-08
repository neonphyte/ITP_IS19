package com.example.meta_glasses_prototype

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var helloText: TextView
    private lateinit var imageView: ImageView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (arePermissionsFullyGranted()) {
            helloText.text = "Full gallery access granted ✅"

            val imageUris = MediaScanner.getImagesFromMetaAIFolder(this)
            helloText.text = "Found ${imageUris.size} image(s) in Meta AI folder"

            if (imageUris.isNotEmpty()) {
                lifecycleScope.launch {
                    var successCount = 0
                    var lastBitmap: android.graphics.Bitmap? = null

                    for (uri in imageUris) {
                        try {
                            val inputStream = contentResolver.openInputStream(uri)
                            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()

                            if (originalBitmap != null) {
                                val blurredBitmap = ImageProcessor.blurFaces(this@MainActivity, originalBitmap)

                                val originalFileName = getFileNameFromUri(this@MainActivity, uri)
                                    ?: "Image_${System.currentTimeMillis()}_${successCount}"

                                FileManager.saveBitmap(
                                    context = this@MainActivity,
                                    bitmap = blurredBitmap,
                                    originalUri = uri,
                                    prefix = "Processed_"
                                )

                                successCount++
                                lastBitmap = blurredBitmap
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    helloText.text = "Processed $successCount image(s) ✅"
                    lastBitmap?.let { imageView.setImageBitmap(it) }
                }
            }
        } else if (Build.VERSION.SDK_INT >= 34) {
            showLimitedAccessDialog()
        } else {
            helloText.text = "Access denied ❌"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        helloText = findViewById(R.id.helloText)
        imageView = findViewById(R.id.imagePreview)
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

    private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        }
        return null
    }
}
