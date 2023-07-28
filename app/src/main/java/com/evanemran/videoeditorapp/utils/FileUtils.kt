package com.evanemran.videoeditorapp.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File


class FileUtils {
    // Get a file from a Uri.
    // Framework Documents, as well as the _data field for the MediaStore and
    // other file-based ContentProviders.
    // @param context The context.
    // @param uri     The Uri to query
    fun getFileFromUri(context: Context, uri: Uri): File {
        var path: String? = null

        // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) { // TODO: 2015. 11. 17. KITKAT

                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        path = Environment.getExternalStorageDirectory().path + "/" + split[1]
                    }

                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    path = getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) { // MediaProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                        "video" -> {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        }
                        "audio" -> {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    path = getDataColumn(context, contentUri, selection, selectionArgs)
                } // MediaStore (and general)
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                path = getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                path = uri.path
            }
            return File(path!!)
    }

    // Get the value of the data column for this Uri. This is useful for
    // MediaStore Uris, and other file-based ContentProviders.
    // @param context       The context.
    // @param uri           The Uri to query.
    // @param selection     (Optional) Filter used in the query.
    // @param selectionArgs (Optional) Selection arguments used in the query.
    // @return The value of the _data column, which is typically a file path.
    fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(
            column
        )
        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index: Int = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            if (cursor != null) cursor.close()
        }
        return null
    }


    // @param uri The Uri to check.
    // @return Whether the Uri authority is ExternalStorageProvide
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    // @param uri The Uri to check.
    // @return Whether the Uri authority is DownloadsProvider.
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    // @param uri The Uri to check.
    // @return Whether the Uri authority is MediaProvider.
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }


    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null

        // Check if the URI uses the content:// scheme
        if (uri.scheme.equals("content", ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    filePath = cursor.getString(columnIndex)
                }
            }
        }

        // If the URI uses the file:// scheme
        if (uri.scheme.equals("file", ignoreCase = true)) {
            filePath = uri.path
        }
        return filePath
    }
}