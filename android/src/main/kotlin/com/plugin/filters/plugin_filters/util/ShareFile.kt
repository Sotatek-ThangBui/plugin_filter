package com.plugin.filters.plugin_filters.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.util.*

class ShareFile constructor(private val context: Context) {
    companion object {
        private val AR_FACES_VIDEO_FOLDER = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "ArFaces"
        )

        private val AR_FACES_PHOTO_FOLDER = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "ArFaces"
        )


        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun saveImageToMediaStorage(bitmap: Bitmap, context: Context): String {
            val appName = "App Name"
            val resolver = context.contentResolver
            val contentValues = ContentValues()
            val fileName = "${appName}_${System.currentTimeMillis()}.png"
            val folder = Environment.DIRECTORY_PICTURES + File.separator + appName + File.separator
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            val imageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let {
                val fos = resolver.openOutputStream(Objects.requireNonNull(imageUri))
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                Objects.requireNonNull(fos)
            }

            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = resolver.query(imageUri!!, projection, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val filePath = cursor.getString(columnIndex)
            cursor.close()
            return filePath
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun saveVideoToMediaStorage(context: Context, filePath: String) {
            val appName = "App Name"
            val videoFile = File(filePath)
            val fileName = appName + System.currentTimeMillis().toString() + ".mp4"
            val contentResolver: ContentResolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            }

            val uri: Uri? =
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                try {
                    val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
                    outputStream?.let {
                        // Write the video data to the output stream.
                        Files.copy(videoFile.toPath(), outputStream)
                        outputStream.close()
                    }
                } catch (_: IOException) {
                }
            }
        }
    }

    private fun isImageType(filePath: String): Boolean {
        return (filePath.endsWith(".png", true) || filePath.endsWith(".jpg", true)
                || filePath.endsWith(".jpeg", true) || filePath.endsWith(".gif", true))
    }

    /*** [filePath] is file path in internal storage*/
    fun saveToGallery(filePath: String) {
        if (isImageType(filePath)) {
            exportFile(File(filePath), AR_FACES_PHOTO_FOLDER)
        } else {
            exportFile(File(filePath), AR_FACES_VIDEO_FOLDER)
        }
    }


    private fun convertFilePathToUri(filePath: String): Uri? {
        return FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".file_provider",
            File(filePath)
        )
    }

    private fun shareImageUri(path: Uri?, type: String?, packageName: String?) {
        if (path != null) {
            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = type
                if (TextUtils.isEmpty(type)) intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_STREAM, path)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
                intent.setPackage(packageName)
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "activity_not_found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun exportFile(src: File, dst: File): File? {
        if (!dst.exists()) {
            if (!dst.mkdir()) {
                return null
            }
        }

        val expFile =
            File((dst.path + File.separator) + getApplicationName() + System.currentTimeMillis() + "_" + src.name)
        var inChannel: FileChannel? = null
        var outChannel: FileChannel? = null
        try {
            inChannel = FileInputStream(src).channel
            outChannel = FileOutputStream(expFile).channel
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        try {
            inChannel?.transferTo(0, inChannel.size(), outChannel)
        } finally {
            inChannel?.close()
            outChannel?.close()
        }
        val name = if (isImageType(src.path)) "Image" else "Video"
        Toast.makeText(context,"Save $name Success ", Toast.LENGTH_SHORT).show()

        MediaScannerConnection.scanFile(context, arrayOf(expFile.path), null, null)
        return expFile
    }

    private fun getApplicationName(): String {
        val applicationInfo: ApplicationInfo = context.applicationInfo
        val stringId: Int = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
            stringId
        )
    }
}