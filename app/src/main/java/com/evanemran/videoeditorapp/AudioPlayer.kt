package com.evanemran.videoeditorapp

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import com.evanemran.videoeditorapp.listeners.AudioPlayerListener
import java.io.File

class AudioPlayer(private val mContext: Context): AudioPlayerListener {

    private var player: MediaPlayer? = null


    override fun playAudio(file: File) {
        MediaPlayer.create(mContext, file.toUri()).apply {
            player = this
            start()
        }
    }

    override fun stopAudio() {
        player?.stop()
        player?.release()

        player = null
    }
}