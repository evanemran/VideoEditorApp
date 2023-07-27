package com.evanemran.videoeditorapp.listeners

import java.io.File

interface AudioPlayerListener {
    fun playAudio(file: File)
    fun stopAudio()
}