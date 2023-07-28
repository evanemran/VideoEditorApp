package com.evanemran.videoeditorapp

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.evanemran.videoeditorapp.listeners.AudioReplaceListener
import com.evanemran.videoeditorapp.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


class AudioManager(
    private val context: Context,
    private val listener: AudioReplaceListener,
    private var originalVideoPath: String,
    private val startTime: String,
    private val duration: String,
    private val newAudioPath: String,
    ) : AsyncTask<Unit, Unit, Boolean>() {

//    private val originalVideoPath = "path/to/original/video.mp4"
//    private val outputPath = "path/to/output/video_with_replaced_audio.mp4"
//    private val startTime = "00:00:10" // Replace with the start time of the segment you want to replace
//    private val duration = "00:00:05" // Replace with the duration of the segment you want to replace
//    private val newAudioPath = "path/to/new/audio.m4a"

    private val root: String = Environment.getExternalStorageDirectory().toString()
    private val app_folder = "$root/VideoEditor/"
    private var outputPath: String = ""

    override fun doInBackground(vararg params: Unit): Boolean {
        try {
            // Step 1: Extract the audio from the video

            var filePrefix = "replaced"
            var fileExtn = ".mp4"

            val filePath: String
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val valuesvideos = ContentValues()
                valuesvideos.put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "Movies/" + "Folder"
                )
                valuesvideos.put(
                    MediaStore.Video.Media.TITLE,
                    filePrefix + System.currentTimeMillis()
                )
                valuesvideos.put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    filePrefix + System.currentTimeMillis() + fileExtn
                )
                valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                valuesvideos.put(
                    MediaStore.Video.Media.DATE_ADDED,
                    System.currentTimeMillis() / 1000
                )
                valuesvideos.put(
                    MediaStore.Video.Media.DATE_TAKEN,
                    System.currentTimeMillis()
                )
                val uri = context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    valuesvideos
                )
                val file: File = FileUtils().getFileFromUri(context, uri!!)
                outputPath = file.absolutePath
            } else {
                filePrefix = "reverse"
                fileExtn = ".mp4"
                var dest: File = File(File(app_folder), filePrefix + fileExtn)
                var fileNo = 0
                while (dest.exists()) {
                    fileNo++
                    dest = File(File(app_folder), filePrefix + fileNo + fileExtn)
                }
                outputPath = dest.absolutePath
            }


            //commands for processbuilder
            val extractAudioCommand = arrayOf(
                "-i", originalVideoPath,
                "-ss", startTime,
                "-t", duration,
                "-c:a", "copy",
                outputPath
            )

            executeFFmpegCommand(extractAudioCommand)
//            executeFfmpegCommandNew(extractAudioCommand.joinToString(" "), outputPath)

            // Step 2: Merge the video with the new audio
            val mergeCommand = arrayOf(
                "-i", originalVideoPath,
                "-i", newAudioPath,
                "-filter_complex", "[0:a][1:a]concat=n=2:v=0:a=1[outa]",
                "-map", "0:v",
                "-map", "[outa]",
                "-c:v", "copy",
                "-shortest",
                outputPath
            )

            executeFFmpegCommand(mergeCommand)
//            executeFfmpegCommandNew(mergeCommand.joinToString(" "), outputPath)

//            replaceAudio(originalVideoPath, newAudioPath, outputPath)

            // Clean up temporary files if needed
            // You can delete the extracted audio file after merging if you don't need it anymore

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun replaceAudio(inputVideoPath: String, inputAudioPath: String, outputVideoPath: String) {
        try {
            val ffmpegCommand = "ffmpeg -i $inputVideoPath -i $inputAudioPath " +
                    "-c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 $outputVideoPath"

            val process = Runtime.getRuntime().exec(ffmpegCommand)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Log the output if needed
                listener.onAudioReplacementComplete(true, outputPath)
            }

            process.waitFor()
            process.destroy()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onPostExecute(success: Boolean) {
        super.onPostExecute(success)

        listener.onAudioReplacementComplete(success, outputPath)
    }

    private fun executeFFmpegCommand(command: Array<String>) {
        try {
            val processBuilder = ProcessBuilder(*command)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Handle FFmpeg output, if needed
            }
            process.waitFor()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


/*    private fun executeFfmpegCommandNew(exe: String, filePath: String) {

        //creating the progress dialog

        *//*
            Here, we have used he Async task to execute our query because if we use the regular method the progress dialog
            won't be visible. This happens because the regular method and progress dialog uses the same thread to execute
            and as a result only one is a allowed to work at a time.
            By using we Async task we create a different thread which resolves the issue.
         *//*
        FFmpegKit.executeAsync(exe, { session ->
            val returnCode = session.returnCode
            if (returnCode.isValueSuccess) {
                //after successful execution of ffmpeg command,
                //again set up the video Uri in VideoView
//                    binding.videoView.setVideoPath(filePath)
                //change the video_url to filePath, so that we could do more manipulations in the
                //resultant video. By this we can apply as many effects as we want in a single video.
                //Actually there are multiple videos being formed in storage but while using app it
                //feels like we are doing manipulations in only one video
                outputPath = filePath
                listener.onAudioReplacementComplete(true, outputPath)
                //play the result video in VideoView
//                    binding.videoView.start()
//                Toast.makeText(context, "Filter Applied", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("TAG", session.allLogsAsString)
//                Toast.makeText(context, "Something Went Wrong!", Toast.LENGTH_SHORT).show()
            }
        }, { log ->
//            Toast.makeText(context, log.message, Toast.LENGTH_LONG).show()
        }) { statistics -> Log.d("STATS", statistics.toString()) }
    }*/

}