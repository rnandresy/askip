package com.rnandresy.lol.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMs: Long = 0L

    /** Démarre l'enregistrement. Retourne le fichier de sortie. */
    fun start(): File {
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file
        startMs    = System.currentTimeMillis()

        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context)
        else
            @Suppress("DEPRECATION") MediaRecorder()
                ).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        return file
    }

    /** Stoppe et retourne (fichier, durée en secondes). Null si erreur. */
    fun stop(): Pair<File, Int>? = runCatching {
        val durationSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt().coerceAtLeast(1)
        recorder?.apply { stop(); release() }
        recorder = null
        val file = outputFile ?: return null
        file to durationSec
    }.getOrElse {
        recorder = null
        null
    }

    /** Annule l'enregistrement et supprime le fichier temporaire. */
    fun cancel() {
        runCatching { recorder?.apply { stop(); release() } }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    val isRecording get() = recorder != null
}