# ITP_IS19 Privacy in Meta Ray-Ban Glasses

An Android app that scans synced photos from Meta Glasses, detects faces in those images, applies a blur effect, and outputs the processed images into a new folder.

##  Features

-  Requests runtime permission for gallery access
-  Scans the **Meta AI** folder inside `Download/`
-  Detects and blurs faces using ML Kit
-  Outputs to a **ProcessedImages** folder without overwriting originals
-  Automatically processes new images when the app is active

---

## 🧱 Project Structure

📁 meta_glasses_prototype/

├── MainActivity.kt # Handles permission and UI trigger for testing

├── MediaScanner.kt # Scans Meta AI folder for new images

├── ImageProcessor.kt # Blurs faces in images

├── FileManager.kt # Saves processed images to new location

├── MetaFolderService.kt: # A persistent Android Service that observes file changes in the Meta AI folder and automatically processes newly added images.

├── res/layout/activity_main.xml

└── AndroidManifest.xml

---

## 📲 How It Works

1. Launch the app and tap the **"Allow Access"** button
2. App requests `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`
3. After permission is granted:
- Checks for new unprocessed images in `Download/Meta AI/`
- Blurs any faces found
- Saves output into `DCIM/ProcessedImages/`

---

## 🛠️ Tech Stack

- Kotlin
- AndroidX
- WorkManager
- ML Kit (for face detection)
- Android Scoped Storage / MediaStore API

---

## 🔐 Permissions

```xml
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
