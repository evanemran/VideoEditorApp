package com.evanemran.videoeditorapp

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val READ_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkRuntimePermission()
    }

    private fun checkRuntimePermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//            != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                READ_EXTERNAL_STORAGE_PERMISSION_CODE)
//        } else {
//            // Permission already granted, proceed with reading the video file
//            readVideoFromInternalStorage()
//        }

        readVideoFromInternalStorage()
    }

    private fun readVideoFromInternalStorage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
//                val inputStream = contentResolver.openInputStream(uri)
                loadVideoFromUri(uri)

            }
        }
    }

    private fun loadVideoFromUri(uri: Uri) {
        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(applicationContext, uri)
            mediaPlayer.setOnPreparedListener {
                val videoView = findViewById<VideoView>(R.id.videoView)
                videoView.setVideoURI(uri)
                videoView.setOnPreparedListener { mp ->
                    mp.start()

                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(applicationContext, uri)

                    val videoLengthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
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
            mediaPlayer.prepareAsync()
        } catch (e: IOException) {
            e.printStackTrace()
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