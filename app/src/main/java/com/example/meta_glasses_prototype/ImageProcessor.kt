package com.example.meta_glasses_prototype

import android.content.Context
import android.graphics.*
import android.renderscript.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

object ImageProcessor {

    suspend fun blurFaces(context: Context, bitmap: Bitmap): Bitmap {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()

        val detector = FaceDetection.getClient(options)
        val faces = detector.process(image).await()

        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()

        for (face in faces) {
            val box = face.boundingBox
            val padding = 30  // increase this value to make box bigger (in pixels)

            val left = (box.left - padding).coerceAtLeast(0)
            val top = (box.top - padding).coerceAtLeast(0)
            val right = (box.right + padding).coerceAtMost(bitmap.width)
            val bottom = (box.bottom + padding).coerceAtMost(bitmap.height)

            val width = right - left
            val height = bottom - top


            if (width <= 0 || height <= 0) continue

            val faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
            val blurredFace = blurWithRenderScript(context, faceBitmap, passes = 6) // multi-pass blur

            canvas.drawBitmap(blurredFace, left.toFloat(), top.toFloat(), paint)
        }

        return resultBitmap
    }

    @Suppress("DEPRECATION")
    private fun blurWithRenderScript(context: Context, bitmap: Bitmap, passes: Int = 1): Bitmap {
        var inputBitmap = bitmap
        var outputBitmap: Bitmap

        val rs = RenderScript.create(context)
        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blur.setRadius(25f) // max radius allowed by RenderScript

        for (i in 0 until passes) {
            val input = Allocation.createFromBitmap(rs, inputBitmap)
            val output = Allocation.createTyped(rs, input.type)

            blur.setInput(input)
            blur.forEach(output)

            outputBitmap = Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, inputBitmap.config ?: Bitmap.Config.ARGB_8888)
            output.copyTo(outputBitmap)

            inputBitmap = outputBitmap

            input.destroy()
            output.destroy()
        }

        rs.destroy()
        return inputBitmap
    }
}
