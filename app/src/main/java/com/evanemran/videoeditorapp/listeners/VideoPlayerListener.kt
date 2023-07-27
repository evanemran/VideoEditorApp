package com.evanemran.videoeditorapp.listeners

import android.net.Uri
import android.widget.VideoView

interface VideoPlayerListener {
    fun playVideo(uri: Uri, videoView: VideoView)
    fun stopVideo()
    fun replaceAudio()
}