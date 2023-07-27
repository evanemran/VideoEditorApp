package com.evanemran.videoeditorapp.listeners

interface AudioReplaceListener {
    fun onAudioReplacementComplete(success: Boolean, outputVideoPath: String)
}