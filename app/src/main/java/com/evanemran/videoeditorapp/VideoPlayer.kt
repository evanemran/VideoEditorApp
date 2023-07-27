package com.evanemran.videoeditorapp

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import com.evanemran.videoeditorapp.listeners.VideoPlayerListener
import kotlinx.android.synthetic.main.activity_main.textView_length
import kotlinx.android.synthetic.main.activity_main.textView_size

class VideoPlayer(private val mContext: Context): VideoPlayerListener {

    private var player: MediaPlayer? = null

    override fun playVideo(uri: Uri, videoView: VideoView) {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(mContext, uri)
        mediaPlayer.setOnPreparedListener {

            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { mp ->
                mp.start()
            }
        }
        mediaPlayer.prepareAsync()
    }

    override fun stopVideo() {
        TODO("Not yet implemented")
    }

    override fun replaceAudio() {
        TODO("Not yet implemented")
    }
}