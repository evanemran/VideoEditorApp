import android.content.Context
import android.util.Log
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode

class FFmpegUtil(
    private val context: Context
) {
    fun replaceAudioOfVideo(inputVideoPath: String, inputAudioPath: String, startTime: String, duration: String, outputVideoPath: String) {
        val ffmpegCommand = arrayOf(
            "-i", inputVideoPath,
            "-i", inputAudioPath,
            "-ss", startTime,
            "-t", duration,
            "-c:v", "copy",
            "-c:a", "aac",
            "-strict", "experimental",
            "-map", "0:v:0",
            "-map", "1:a:0",
            outputVideoPath
        )

        // Clear previous FFmpegKit sessions and configure FFmpegKit
//        FFmpegKitConfig.reset()
        FFmpegKitConfig.enableLogCallback { message -> /* Handle FFmpegKit log messages */ }
        FFmpegKitConfig.enableStatisticsCallback { session -> /* Handle FFmpegKit session statistics */ }

        // Execute the FFmpeg command using FFmpegKit
        val session = FFmpegKit.execute(ffmpegCommand.joinToString(" "))

        // Wait for the session to finish
        FFmpegKit.executeAsync(ffmpegCommand.joinToString(" ")) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                // FFmpeg command execution is successful
                // Handle the output video file here
                Log.e("RESULT", "Success")
//                Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
            }
            else {
                Log.e("RESULT", "Failed")
                // FFmpeg command execution failed
                // Handle the error here
//                Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}