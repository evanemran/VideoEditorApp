package com.evanemran.videoeditorapp

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.evanemran.videoeditorapp.utils.FileSelectionUtils
import com.evanemran.videoeditorapp.utils.FileUtils
import kotlinx.android.synthetic.main.activity_main.button_open
import kotlinx.android.synthetic.main.activity_main.editTextEnd
import kotlinx.android.synthetic.main.activity_main.editTextStart
import kotlinx.android.synthetic.main.activity_main.playRecordingButton
import kotlinx.android.synthetic.main.activity_main.rangeSeekBar
import kotlinx.android.synthetic.main.activity_main.replaceAudioButton
import kotlinx.android.synthetic.main.activity_main.textView_end
import kotlinx.android.synthetic.main.activity_main.textView_length
import kotlinx.android.synthetic.main.activity_main.textView_size
import kotlinx.android.synthetic.main.activity_main.textView_start
import kotlinx.android.synthetic.main.activity_main.toggleRecordingButton
import kotlinx.android.synthetic.main.activity_main.videoView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_VIDEO_PICK = 1001
    private val REQUEST_CODE_PERMISSIONS = 1002

    lateinit var recorder: AudioRecorder
    lateinit var audioPlayer: AudioPlayer
    lateinit var videoPlayer: VideoPlayer
    private var audioFile: File? = null
    private var videoFile: String = ""
    private var isRecording: Boolean = false
    private lateinit var r: Runnable
    private var videoFilePath: String = ""

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.setMessage("Please wait..")
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)

        checkRuntimePermission()

        recorder = AudioRecorder(applicationContext)
        audioPlayer = AudioPlayer(applicationContext)
        videoPlayer = VideoPlayer(applicationContext)

        button_open.setOnClickListener {
            readVideoFromInternalStorage()
        }

        toggleRecordingButton.setOnClickListener {
            if(isRecording) {
                isRecording = false
                toggleRecordingButton.setImageResource(R.drawable.ic_mic)
                Toast.makeText(applicationContext, "Recording Stopped", Toast.LENGTH_SHORT).show()
                recorder.stopRecording()
                playRecordingButton.isEnabled = true
            }
            else {
                isRecording = true
                Toast.makeText(applicationContext, "Recording Started", Toast.LENGTH_SHORT).show()
                playRecordingButton.isEnabled = false
                toggleRecordingButton.setImageResource(R.drawable.ic_mic_off)
                File(cacheDir, "audio.mp3").also {
                    recorder.startRecording(it)
                    audioFile = it
                }
            }
        }

        playRecordingButton.setOnClickListener {
            audioPlayer.playAudio(audioFile ?: return@setOnClickListener)
        }

        videoView.setOnPreparedListener {


            val duration = it.duration / 1000

            textView_start.text = "00:00:00"
            textView_end.text = getTime(it.duration / 1000)

            it.isLooping = true

            ///Seekbar range settings not working///

//            rangeSeekBar.setRangeValues(0, duration)
//            rangeSeekBar.setSelectedMinValue(0)
//            rangeSeekBar.setSelectedMaxValue(duration)
//            rangeSeekBar.selectedMinValue = 0
//            rangeSeekBar.selectedMaxValue = duration

            rangeSeekBar.isEnabled = true

            rangeSeekBar!!.setOnRangeSeekBarChangeListener { bar, minValue, maxValue ->
                videoView.seekTo(minValue as Int * 1000)

                textView_start.text = getTime(bar.selectedMinValue as Int)
                textView_end.text = getTime(bar.selectedMaxValue as Int)
            }

            val handler = Handler()
            r = Runnable {
                if (videoView.currentPosition >= rangeSeekBar.selectedMaxValue.toInt() * 1000) videoView.seekTo(
                    rangeSeekBar.selectedMinValue.toInt() * 1000
                )
                handler.postDelayed(r, 1000)
            }
            handler.postDelayed(r.also { r = it }, 1000)
        }

        replaceAudioButton.setOnClickListener {

            if(editTextStart.text.toString().isEmpty() || editTextEnd.text.toString().isEmpty()
                || !audioFile!!.exists()) {
                return@setOnClickListener
            }

            progressDialog.show()

            val startingTimeStamp = editTextStart.text.toString()
            val endingTimeStamp = editTextEnd.text.toString()
            val duration = getTimeDifference(startingTimeStamp, endingTimeStamp)


            val videoOutputFilePath = getVideoOutputFilePath()
            val inputDirectory = Environment.getExternalStoragePublicDirectory("videoo.mp4")

            val ffmpegReplaceCommand = "-f mp4 -i ${videoFile} -i ${audioFile!!.absolutePath} -ss $startingTimeStamp -t $duration -c:v copy -c:a copy -map 0:v:0 -map 1:a:0 $videoOutputFilePath"

            val ffmpegSingleMergeCommand = "-f mp4 -i ${inputDirectory.absolutePath} -i ${audioFile!!.absolutePath} -ss 00:00:00 -t 00:00:10 -c:v copy -c:a copy -map 0:v:0 -map 1:a:0 -t 00:00:05 -c:a copy -map 1:a:1 $videoOutputFilePath"

            val rc: Int = FFmpeg.execute(ffmpegReplaceCommand)
            //  val rc: Int = FFmpeg.execute("-i " + outputDirectory.absolutePath + " -c:a copy " + audioOutputFilePath)
            when (rc) {
                RETURN_CODE_SUCCESS -> {
                    // FFmpeg command execution completed successfully
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Success! Saved to $videoOutputFilePath", Toast.LENGTH_SHORT).show()
                }
                RETURN_CODE_CANCEL -> {
                    progressDialog.dismiss()
                    // FFmpeg command execution cancelled by user
                    Toast.makeText(this@MainActivity, "Cancel", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    progressDialog.dismiss()
                    // Something went wrong
                    Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readVideoFromInternalStorage()
            }
        }
    }

    private fun checkRuntimePermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            0
        )
    }

    private fun readVideoFromInternalStorage() {
        if (ContextCompat.checkSelfPermission(
                this,
                READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "video/*"
            startActivityForResult(intent, REQUEST_CODE_VIDEO_PICK)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_VIDEO_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { videoUri ->

                try {

                    videoView.setVideoURI(videoUri)
                    videoView.start()

                } catch (e: java.lang.Exception) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

                //ToDo
                videoFilePath = videoUri.path!!

                val fileUri = data.data
                val file = File(fileUri!!.path.toString()) //create path from uri
                val split = file.path.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray() //split the path.

                videoFile = FileSelectionUtils.getFilePathFromUri(applicationContext, videoUri).path!!


                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(applicationContext, videoUri)

                val videoLengthStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val videoLengthInMillis = videoLengthStr?.toLongOrNull() ?: 0


                val duration = formatMillisecondsToTime(videoLengthInMillis)

                val contentResolver: ContentResolver = applicationContext.contentResolver

                val inputStream = contentResolver.openInputStream(videoUri)
                val fileSize = inputStream?.available() ?: 0
                val fileSizeInMB = String.format("Size %.2f MB", fileSize / (1024.0 * 1024.0))

                inputStream?.close()
                retriever.release()


                textView_length.text = duration
                textView_size.text = fileSizeInMB
            }
        }
    }
    private fun getVideoOutputFilePath(): String {
        val outputDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        outputDirectory.mkdirs()
        val outputFile = File(outputDirectory, "test3.mp4")
        if (audioFile!!.exists()) {
            val deleted: Boolean = outputFile.delete()
            if (deleted) {
                Log.d("FileDeleter", "File deleted successfully")
            } else {
                Log.d("FileDeleter", "Failed to delete file")
            }
        }
        return outputFile.absolutePath
    }
    fun formatMillisecondsToTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    private fun getTime(seconds: Int): String {
        val hr = seconds / 3600
        val rem = seconds % 3600
        val mn = rem / 60
        val sec = rem % 60
        return String.format("%02d", hr) + ":" + String.format(
            "%02d",
            mn
        ) + ":" + String.format("%02d", sec)
    }
    fun getTimeDifference(startTime: String, endTime: String): String {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val startTimeMillis = timeFormat.parse(startTime)?.time ?: 0
        val endTimeMillis = timeFormat.parse(endTime)?.time ?: 0

        val differenceMillis = endTimeMillis - startTimeMillis
        val differenceSeconds = differenceMillis / 1000

        val hours = differenceSeconds / 3600
        val minutes = (differenceSeconds % 3600) / 60
        val seconds = differenceSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}