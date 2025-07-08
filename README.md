# ITP_IS19 Privacy in Meta Ray-Ban Glasses

An Android app that scans synced photos from Meta Glasses, detects faces in those images, applies a blur effect, and outputs the processed images into a new folder — all done seamlessly in the background. 

##  Features

-  Requests runtime permission for gallery access
-  Scans the **Meta AI** folder inside `Download/`
-  Detects and blurs faces using ML Kit
-  Outputs to a **ProcessedImages** folder without overwriting originals
-  ~~Automatically processes new images every 15 minutes using `WorkManager`~~
-  ~~Runs in the background (even after swiping the app away)~~

---

## 🧱 Project Structure

📁 meta_glasses_prototype/

├── MainActivity.kt # Handles permission and UI trigger for testing

├── MediaScanner.kt # Scans Meta AI folder for new images

├── ImageProcessor.kt # Blurs faces in images

├── FileManager.kt # Saves processed images to new location

├── FaceBlurWorker.kt # Background worker to automate scanning and processing

├── res/layout/activity_main.xml

└── AndroidManifest.xml

---

## 📲 How It Works

1. Launch the app and tap the **"Allow Access"** button
2. App requests `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`
3. After permission is granted:
- Runs background face detection every 15 minutes
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
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
