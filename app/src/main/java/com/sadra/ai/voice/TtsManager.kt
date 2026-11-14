package com.sadra.ai.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    var persianAvailable = false
        private set
    private var onDone: (() -> Unit)? = null
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val r = tts?.setLanguage(Locale("fa", "IR"))
            persianAvailable = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            ready = true
        }
    }

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finish()
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) = finish()
        })
    }

    private fun finish() {
        main.post { onDone?.also { onDone = null }?.invoke() }
    }

    /** Speaks [text]; [done] is always invoked (also when Persian TTS is unavailable). */
    fun speak(text: String, done: () -> Unit) {
        if (!ready || !persianAvailable) { done(); return }
        onDone = done
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "sadra_${System.nanoTime()}")
    }

    /** Speech interruption (barge-in). */
    fun stop() {
        onDone = null
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop(); tts?.shutdown(); tts = null
    }
}
