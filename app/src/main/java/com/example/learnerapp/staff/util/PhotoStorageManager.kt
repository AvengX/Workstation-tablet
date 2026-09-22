package com.example.learnerapp.staff.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utility for local, private photo storage for task steps.
 * Keeps photos within the app's internal filesDir ("task_photos"),
 * ensuring permanent availability without remote cloud dependencies or expiring URI permissions.
 */
object PhotoStorageManager {

    private const val DIRECTORY_NAME = "task_photos"

    /**
     * Copies a chosen image from [sourceUri] into app-private storage.
     * Returns the absolute file path on success, or null on error.
     */
    fun savePhotoFromUri(context: Context, sourceUri: Uri): String? {
        return try {
            val photosDir = File(context.filesDir, DIRECTORY_NAME).apply {
                if (!exists()) mkdirs()
            }
            val fileName = "step_photo_${UUID.randomUUID().toString().replace("-", "").take(16)}.jpg"
            val targetFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a local photo file if it exists.
     */
    fun deletePhotoFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Safely decodes a local bitmap with in-memory downsampling suitable for tablet displays.
     */
    fun loadBitmap(path: String?, targetWidth: Int = 800, targetHeight: Int = 600): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var sampleSize = 1
            if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / sampleSize) >= targetHeight && (halfWidth / sampleSize) >= targetWidth) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
