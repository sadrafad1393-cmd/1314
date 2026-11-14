package com.sadra.ai.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerManager(private val ctx: Context, private val cb: Callback) {

    interface Callback {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onRms(level: Float)
        fun onError(message: String, fatal: Boolean)
    }

    private var recognizer: SpeechRecognizer? = null

    /** Prefer Google's recognizer. If SADRA itself becomes the "voice input" provider, the default recognizer would be our stub. */
    private fun pickService(): ComponentName? {
        val list = ctx.packageManager.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
            .map { it.serviceInfo }.filter { it.packageName != ctx.packageName }
        val s = list.firstOrNull { it.packageName == "com.google.android.googlequicksearchbox" } ?: list.firstOrNull()
        return s?.let { ComponentName(it.packageName, it.name) }
    }

    fun start() {
        val service = pickService()
        if (service == null && !SpeechRecognizer.isRecognitionAvailable(ctx)) {
            cb.onError("سرویس تشخیص گفتار روی این گوشی نصب نیست (برنامه‌ی Google را نصب/به‌روزرسانی کنید).", true)
            return
        }
        if (recognizer == null) {
            recognizer = (if (service != null) SpeechRecognizer.createSpeechRecognizer(ctx, service)
            else SpeechRecognizer.createSpeechRecognizer(ctx)).apply { setRecognitionListener(listener) }
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        recognizer?.startListening(i)
    }

    fun stop() { recognizer?.stopListening() }
    fun cancel() { recognizer?.cancel() }
    fun destroy() { recognizer?.destroy(); recognizer = null }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) { cb.onRms(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(cb::onPartial)
        }
        override fun onResults(results: Bundle?) {
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (t.isNullOrBlank()) cb.onError("چیزی متوجه نشدم، دوباره بگویید.", false) else cb.onFinal(t)
        }
        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> cb.onError("صدایی نشنیدم.", false)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> cb.onError("دسترسی میکروفون داده نشده است.", true)
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    cb.onError("تشخیص گفتار فارسی به اینترنت نیاز دارد (یا بسته‌ی فارسی آفلاین را در تنظیمات Google نصب کنید).", true)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> cb.onError("تشخیص گفتار مشغول است، دوباره امتحان کنید.", false)
                else -> cb.onError("خطا در تشخیص گفتار (کد $error).", true)
            }
        }
    }
}
