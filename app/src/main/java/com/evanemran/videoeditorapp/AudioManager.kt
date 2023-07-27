package com.evanemran.videoeditorapp

import android.os.AsyncTask
import com.evanemran.videoeditorapp.listeners.AudioReplaceListener
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class AudioManager(
    private val listener: AudioReplaceListener,
    private val originalVideoPath: String,
    private val outputPath: String,
    private val startTime: String,
    private val duration: String,
    private val newAudioPath: String,
    ) : AsyncTask<Unit, Unit, Boolean>() {

//    private val originalVideoPath = "path/to/original/video.mp4"
//    private val outputPath = "path/to/output/video_with_replaced_audio.mp4"
//    private val startTime = "00:00:10" // Replace with the start time of the segment you want to replace
//    private val duration = "00:00:05" // Replace with the duration of the segment you want to replace
//    private val newAudioPath = "path/to/new/audio.m4a"

    override fun doInBackground(vararg params: Unit): Boolean {
        try {
            // Step 1: Extract the audio from the video
            val extractAudioCommand = arrayOf(
                "-i", originalVideoPath,
                "-ss", startTime,
                "-t", duration,
                "-c:a", "copy",
                outputPath
            )

            executeFFmpegCommand(extractAudioCommand)

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

            // Clean up temporary files if needed
            // You can delete the extracted audio file after merging if you don't need it anymore

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun onPostExecute(success: Boolean) {
        super.onPostExecute(success)

        val sourceFile = File(outputPath)
        val destinationFile = File(outputPath, sourceFile.name)

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

}