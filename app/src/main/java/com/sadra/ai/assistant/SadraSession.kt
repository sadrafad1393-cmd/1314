package com.sadra.ai.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.sadra.ai.MainActivity
import com.sadra.ai.ai.AssistantEngine
import com.sadra.ai.memory.AppDatabase
import com.sadra.ai.memory.ChatMessage
import com.sadra.ai.settings.Prefs
import com.sadra.ai.ui.WaveView
import com.sadra.ai.voice.SpeechRecognizerManager
import com.sadra.ai.voice.TtsManager
import kotlinx.coroutines.*

/**
 * The glass bottom sheet shown when the user triggers the assistant (e.g. long-press side button, where the OEM maps it).
 * Built with plain Views (no Compose) so it appears fast and stays light on 2 GB devices.
 */
class SadraSession(context: Context) : VoiceInteractionSession(context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs = Prefs(context)
    private val engine = AssistantEngine(context, prefs)
    private val dao = AppDatabase.get(context).chatDao()
    private var tts: TtsManager? = null
    private var speech: SpeechRecognizerManager? = null
    private var listening = false

    private lateinit var status: TextView
    private lateinit var reply: TextView
    private lateinit var wave: WaveView
    private lateinit var mic: TextView

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        tts = TtsManager(context)
        speech = SpeechRecognizerManager(context, object : SpeechRecognizerManager.Callback {
            override fun onPartial(text: String) { status.text = text }
            override fun onRms(level: Float) { wave.level = level }
            override fun onFinal(text: String) { setListening(false); status.text = text; handle(text) }
            override fun onError(message: String, fatal: Boolean) { setListening(false); status.text = message }
        })
    }

    override fun onCreateContentView(): View {
        val root = FrameLayout(context).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0x55000000)
            setOnClickListener { hide() }
        }
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            val r = dp(28).toFloat()
            background = GradientDrawable().apply {
                setColor(0xCC0F1626.toInt())
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setStroke(dp(1), 0x5500E5FF)
            }
            setPadding(dp(20), dp(18), dp(20), dp(20))
            @Suppress("DEPRECATION")
            setOnApplyWindowInsetsListener { v, ins ->
                v.setPadding(dp(20), dp(18), dp(20), dp(20) + ins.systemWindowInsetBottom); ins
            }
        }
        val avatar = TextView(context).apply {
            text = "ص"; textSize = 20f; setTextColor(0xFF00212A.toInt()); gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF00E5FF.toInt(), 0xFF2979FF.toInt())).apply { shape = GradientDrawable.OVAL }
        }
        panel.addView(avatar, LinearLayout.LayoutParams(dp(44), dp(44)))
        status = TextView(context).apply { text = "در حال گوش دادن…"; textSize = 17f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        panel.addView(status, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })
        wave = WaveView(context)
        panel.addView(wave, LinearLayout.LayoutParams(MATCH_PARENT, dp(44)).apply { topMargin = dp(8) })
        reply = TextView(context).apply { textSize = 15f; setTextColor(0xFFB8C7DC.toInt()); gravity = Gravity.CENTER }
        panel.addView(reply, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(6) })
        mic = TextView(context).apply {
            text = "🎙"; textSize = 26f; gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF00E5FF.toInt(), 0xFF2979FF.toInt())).apply { shape = GradientDrawable.OVAL }
            setOnClickListener { if (listening) speech?.stop() else { tts?.stop(); startListening() } }
        }
        panel.addView(mic, LinearLayout.LayoutParams(dp(64), dp(64)).apply { topMargin = dp(10) })
        root.addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        applyBlur()
        reply.text = ""
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            // A session cannot show a runtime-permission dialog; hand over to the app screen that can.
            startAssistantActivity(
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_AUTO_LISTEN, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        startListening()
    }

    /** Real background blur on Android 12+ devices that support it; others just get the translucent dim. */
    private fun applyBlur() {
        if (Build.VERSION.SDK_INT < 31) return
        try {
            window.window?.let { w ->
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                w.attributes = w.attributes.apply { blurBehindRadius = 40 }
            }
        } catch (_: Exception) { }
    }

    private fun setListening(on: Boolean) { listening = on; wave.active = on }

    private fun startListening() {
        status.text = "در حال گوش دادن…"
        setListening(true)
        speech?.start()
    }

    private fun handle(text: String) {
        scope.launch {
            dao.insert(ChatMessage(role = "user", text = text))
            val out = engine.process(text, dao.recent(12).reversed())
            dao.insert(ChatMessage(role = "assistant", text = out.message))
            reply.text = out.message
            if (prefs.speakReplies) tts?.speak(out.message) { if (out.dismiss) hide() }
            else if (out.dismiss) hide()
        }
    }

    override fun onHide() {
        super.onHide()
        speech?.cancel(); tts?.stop(); setListening(false)
        if (!prefs.saveHistory) scope.launch { dao.clear() }
    }

    override fun onDestroy() {
        speech?.destroy(); tts?.shutdown(); scope.cancel()
        super.onDestroy()
    }
}
