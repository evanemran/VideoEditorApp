package com.evanemran.videoeditorapp.listeners

import java.io.File

interface AudioRecorderListener {
    fun startRecording(file: File)
    fun stopRecording()
}