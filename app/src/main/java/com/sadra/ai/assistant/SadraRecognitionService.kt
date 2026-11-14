package com.sadra.ai.assistant

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Android requires an assistant to declare a RecognitionService. SADRA uses Google's recognizer for actual Persian STT
 * (see SpeechRecognizerManager), so this service honestly reports that it cannot serve third-party clients.
 */
class SadraRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(SpeechRecognizer.ERROR_CLIENT)
    }
    override fun onCancel(listener: Callback?) {}
    override fun onStopListening(listener: Callback?) {}
}
