/*
 *
 *  * Copyright (C) 2025 Kaori (original author)
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.kaori.circletosearch.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    private const val SCREENSHOT_FILENAME = "screenshot.png"

    fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String = SCREENSHOT_FILENAME): String {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun loadBitmap(path: String): Bitmap? {
        return BitmapFactory.decodeFile(path)
    }

    fun cropBitmap(source: Bitmap, rect: Rect): Bitmap {
        // Ensure rect is within bounds
        val left = rect.left.coerceIn(0, source.width)
        val top = rect.top.coerceIn(0, source.height)
        val width = rect.width().coerceAtMost(source.width - left)
        val height = rect.height().coerceAtMost(source.height - top)
        
        return if (width > 0 && height > 0) {
            Bitmap.createBitmap(source, left, top, width, height)
        } else {
            source // Fallback or handle error
        }
    }

    fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
        try {
            if (source.width <= maxLength && source.height <= maxLength) return source
            val aspectRatio = source.width.toDouble() / source.height.toDouble()
            val targetWidth = if (aspectRatio >= 1) maxLength else (maxLength * aspectRatio).toInt()
            val targetHeight = if (aspectRatio < 1) maxLength else (maxLength / aspectRatio).toInt()
            return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        } catch (e: Exception) {
            return source
        }
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        val contentResolver = context.contentResolver
        var imageUri: android.net.Uri? = null
        var completed = false
        try {
            val filename = "CircleSelection_${System.currentTimeMillis()}.png"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(
                    android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_PICTURES + "/CircleToSearch"
                )
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val pendingUri = contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return false
            imageUri = pendingUri
            val wroteImage = contentResolver.openOutputStream(pendingUri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } ?: false
            if (!wroteImage) return false

            contentValues.clear()
            contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(pendingUri, contentValues, null, null)
            completed = true
            return true
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "Failed to save to gallery", e)
            return false
        } finally {
            if (!completed) imageUri?.let { contentResolver.delete(it, null, null) }
        }
    }
}
