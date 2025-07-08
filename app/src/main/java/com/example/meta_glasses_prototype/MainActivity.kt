package com.example.meta_glasses_prototype

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.meta_glasses_prototype.MediaScanner

class MainActivity : AppCompatActivity() {

    private lateinit var helloText: TextView
    private lateinit var imageView: ImageView

    // Request permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (arePermissionsFullyGranted()) {
            helloText.text = "Full gallery access granted ✅"

            // MediaScanner running
            val imageUris = MediaScanner.getImagesFromMetaAIFolder(this)
            helloText.text = "Found ${imageUris.size} image(s) in Meta AI folder"

            if (imageUris.isNotEmpty()) {
                imageView.setImageURI(imageUris[0]) // Show preview
            }
        } else if (Build.VERSION.SDK_INT >= 34) {
            // Show dialog only if Android 14+ (API 34) and permission partially granted
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
}
