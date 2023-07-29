package com.evanemran.videoeditorapp.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileSelectionUtils {
    @Throws(IOException::class)
    fun copyStream(source: InputStream, target: OutputStream) {
        val buf = ByteArray(8192)
        var length: Int
        while (source.read(buf).also { length = it } != -1) {
            target.write(buf, 0, length)
        }
    }

    @Throws(IOException::class)
    fun getFilePathFromUri(context: Context, uri: Uri?): Uri {
        val fileName = getFileName(context, uri)
        val file = File(context.externalCacheDir, fileName)
        file.createNewFile()
        FileOutputStream(file).use { outputStream ->
            context.contentResolver.openInputStream(
                uri!!
            ).use { inputStream ->
                copyStream(inputStream!!, outputStream) //Simply reads input to output stream
                outputStream.flush()
            }
        }
        return Uri.fromFile(file)
    }

    fun getFileName(context: Context, uri: Uri?): String {
        var fileName = getFileNameFromCursor(context, uri)
        if (fileName == null) {
            val fileExtension = getFileExtension(context, uri)
            fileName = "temp_file" + if (fileExtension != null) ".$fileExtension" else ""
        } else if (!fileName.contains(".")) {
            val fileExtension = getFileExtension(context, uri)
            fileName = "$fileName.$fileExtension"
        }
        return fileName
    }

    fun getFileExtension(context: Context, uri: Uri?): String? {
        val fileType = context.contentResolver.getType(uri!!)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
    }

    fun getFileNameFromCursor(context: Context, uri: Uri?): String? {
        val fileCursor = context.contentResolver.query(
            uri!!,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        var fileName: String? = null
        if (fileCursor != null && fileCursor.moveToFirst()) {
            val cIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cIndex != -1) {
                fileName = fileCursor.getString(cIndex)
            }
        }
        return fileName
    }
}