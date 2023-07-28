package com.evanemran.videoeditorapp

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.evanemran.videoeditorapp.listeners.AudioReplaceListener
import com.evanemran.videoeditorapp.utils.FileUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.net.URI


class MainActivity : AppCompatActivity() {

    private val READ_REQUEST_CODE = 1

    private val REQUEST_PERMISSION = 101
    private val SETTINGS_CODE = 102

    lateinit var recorder: AudioRecorder
    lateinit var audioPlayer: AudioPlayer
    lateinit var videoPlayer: VideoPlayer
    private var audioFile: File? = null
    private var videoFile: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkRuntimePermission()

        recorder = AudioRecorder(applicationContext)
        audioPlayer = AudioPlayer(applicationContext)
        videoPlayer = VideoPlayer(applicationContext)

        button_open.setOnClickListener {
            readVideoFromInternalStorage()
        }

        startRecordingButton.setOnClickListener {
            File(cacheDir, "audio.mp3").also {
                recorder.startRecording(it)
                audioFile = it
            }
        }

        stopRecordingButton.setOnClickListener {
            recorder.stopRecording()
        }

        playRecordingButton.setOnClickListener {
            audioPlayer.playAudio(audioFile ?: return@setOnClickListener)
        }

        replaceAudioButton.setOnClickListener {

            AudioManager(
                applicationContext,
                object : AudioReplaceListener {
                    override fun onAudioReplacementComplete(success: Boolean, outputVideoPath: String) {
                        if (success) {
                            // Audio replacement was successful, you can use the new video with replaced audio
                            try {
                                val outputVideoFile: File = File(outputVideoPath)
                                val internalStorageDir =
                                    filesDir // You can change this to any other directory as per your requirements

                                // Copy the output video file to internal storage
                                val outputFileInInternalStorage =
                                    File(internalStorageDir, outputVideoFile.name)
                                org.apache.commons.io.FileUtils.copyFile(outputVideoFile, outputFileInInternalStorage)

                                // Now the new video with replaced audio is saved in the internal storage.
                            } catch (e: IOException) {
                                e.printStackTrace()
                                // Handle the exception if copying the file fails.
                            }

                            Toast.makeText(this@MainActivity, "Audio Replaced!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                videoFile,
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "00:00:10",
                "00:00:05",
                audioFile!!.absolutePath
            ).execute()



//            replaceAudioTask.execute()
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun hasPermission() {
        val s = arrayOf<String>(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        )
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            PermissionsDialog(s)
        } else {
            // do something when you got permission
        }
    }

    private fun PermissionsDialog(s: Array<String>) {
        ActivityCompat.requestPermissions(this@MainActivity, s, REQUEST_PERMISSION)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.size > 0) {
                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this@MainActivity,
                                permissions[0]!!
                            )
                        ) {
                            startActivityForResult(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                                ), SETTINGS_CODE
                            )
                        } else {
                            hasPermission()
                        }
                    } else {
                        // do something when you got permission
                    }
                }
            }
        }
    }


    fun getPathFromUri(context: Context, uri: Uri): String {
        var filePath: String? = null

        // MediaStore (for images and videos) or other content providers
        if (uri.scheme == "content") {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    filePath = cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
                Log.e("getPathFromUri", "Error getting path from content URI: $uri", e)
            } finally {
                cursor?.close()
            }
        }

        // File scheme
        else if (uri.scheme == "file") {
            filePath = uri.path
        }

        return filePath.toString()
    }
//
//    private val replaceAudioTask = AudioManager(
//        object : AudioReplaceListener {
//            override fun onAudioReplacementComplete(success: Boolean, outputVideoPath: String) {
//                if (success) {
//                    // Audio replacement was successful, you can use the new video with replaced audio
//                    Toast.makeText(this@MainActivity, "Audio Replaced!", Toast.LENGTH_LONG).show()
//                } else {
//                    Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_LONG).show()
//                }
//            }
//        },
//        videoFile!!.absolutePath,
//        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
//        "00:00:10",
//        "00:00:05",
//        audioFile!!.absolutePath
//    )

    private fun checkRuntimePermission() {

//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(
//                android.Manifest.permission.RECORD_AUDIO,
//                android.Manifest.permission.READ_EXTERNAL_STORAGE,
//                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            ),
//            0
//        )

//        hasPermission()

//        readVideoFromInternalStorage()
    }

    private fun readVideoFromInternalStorage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"

        startActivityForResult(intent, READ_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SETTINGS_CODE) {
//            hasPermission()
        }

        else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
//                val inputStream = contentResolver.openInputStream(uri)
                videoPlayer.playVideo(uri, videoView)

//                val fileUri = data.data
//                val file = File(fileUri!!.path.toString()) //create path from uri
//
//                val split = file.path.split(":".toRegex()).dropLastWhile { it.isEmpty() }
//                    .toTypedArray() //split the path.
//
//                videoFile = split[1] //assign it to a string(your choice).

                videoFile = FileUtils().getFilePathFromUri(applicationContext, uri)!!

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(applicationContext, uri)

                val videoLengthStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val videoLengthInMillis = videoLengthStr?.toLongOrNull() ?: 0


                val duration = formatMillisecondsToTime(videoLengthInMillis)

                val contentResolver: ContentResolver = applicationContext.contentResolver

                val inputStream = contentResolver.openInputStream(uri)
                val fileSize = inputStream?.available() ?: 0
                val fileSizeInMB = String.format("%.2f MB", fileSize / (1024.0 * 1024.0))

                inputStream?.close()
                retriever.release()


                textView_length.text = duration
                textView_size.text = fileSizeInMB
            }
        }
    }

    fun formatMillisecondsToTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}