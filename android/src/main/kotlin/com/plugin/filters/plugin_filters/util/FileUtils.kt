package com.plugin.filters.plugin_filters.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import java.io.*
import java.util.*

fun copyVideoFileFromAsset(context: Context, assetFilePath: String, outFilePath: String): String {
    // Get the asset file input stream
    val inputStream = context.assets.open(assetFilePath)
    // Get the private files directory and create the output file
    val outputFile = File(outFilePath)
    if (outputFile.exists()) {
        return outputFile.path
    }

    inputStream.use {
        // Open the output file output stream
        val outputStream = FileOutputStream(outputFile)
        // Copy the bytes from the input stream to the output stream
        inputStream.copyTo(outputStream)
        // Flush and close the output stream
        outputStream.flush()
        outputStream.close()
        return outputFile.path
    }
}

fun copyFile(source: File, fos: OutputStream): Boolean {
    try {
        val fis = FileInputStream(source)
        val buff = ByteArray(2048)
        var len = -1
        while (fis.read(buff).also { len = it } != -1) {
            fos.write(buff, 0, len)
        }
        fos.flush()
        fis.close()
        fos.close()
        return true
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return false
}

fun copyFile(source: File, dest: File): Boolean {
    try {
        dest.parentFile?.mkdirs()
        val fis = FileInputStream(source)
        val fos = FileOutputStream(dest)
        val buff = ByteArray(2048)
        var len = -1
        while (fis.read(buff).also { len = it } != -1) {
            fos.write(buff, 0, len)
        }
        fos.flush()
        fis.close()
        fos.close()
        return true
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return false
}

fun saveFile(body: ResponseBody?, path: String): String {
    if (body == null)
        return ""
    var input: InputStream? = null
    try {
        input = body.byteStream()
        val fos = FileOutputStream(path)
        fos.use { output ->
            val buffer = ByteArray(4 * 1024) // or other buffer size
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
            output.flush()
        }
        return path
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        input?.close()
    }
    return ""
}

fun getUri(context: Context, file: File?): Uri? {
    if (file != null) {
        val photoURI: Uri = if (Build.VERSION.SDK_INT >= 24) {
            FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        } else {
            Uri.fromFile(file)
        }
        return photoURI
    }
    return null
}

fun readTextFile(filePath: String?): String? {
    try {
        val br = BufferedReader(FileReader(filePath))
        var tmp: String
        var line: String? = null
        while (br.readLine().also { tmp = it } != null) {
            line = if (line == null) tmp else """
     $line$tmp
     
     """.trimIndent()
        }
        br.close()
        return line
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

fun saveToFile(text: String?, dest: File): Boolean {
    try {
        if (dest.parentFile != null && !dest.parentFile.exists()) {
            dest.parentFile.mkdirs()
        }
        val pw = PrintWriter(dest)
        pw.print(text)
        pw.flush()
        pw.close()
        return true
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }
    return false
}

fun getMimeType(context: Context, uri: Uri): String? {
    val mimeType: String? = if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
        val cr: ContentResolver = context.contentResolver
        cr.getType(uri)
    } else {
        val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileExtension.lowercase(Locale.getDefault())
        )
    }

    return mimeType
}