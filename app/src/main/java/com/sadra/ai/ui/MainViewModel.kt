package com.sadra.ai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.ai.ai.Command
import com.sadra.ai.ai.CommandParser
import com.sadra.ai.devicecontrol.DeviceController
import com.sadra.ai.memory.AppDatabase
import com.sadra.ai.memory.ChatMessage
import com.sadra.ai.network.AiClient
import com.sadra.ai.settings.Prefs
import com.sadra.ai.voice.SpeechRecognizerManager
import com.sadra.ai.voice.TtsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val listening: Boolean = false,
    val partial: String = "",
    val level: Float = 0f,
    val busy: Boolean = false,
    val notice: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val dao = AppDatabase.get(app).chatDao()
    private val device = DeviceController(app)
    private val client = AiClient(prefs)
    private val tts = TtsManager(app)
    val sub = com.sadra.ai.billing.SubscriptionManager.get(app)
    val access = sub.state
    val subNotice = sub.notice

    val messages: StateFlow<List<ChatMessage>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val speech = SpeechRecognizerManager(app, object : SpeechRecognizerManager.Callback {
        override fun onPartial(text: String) = _ui.update { it.copy(partial = text) }
        override fun onRms(level: Float) = _ui.update { it.copy(level = level) }
        override fun onFinal(text: String) {
            _ui.update { it.copy(listening = false, partial = "", level = 0f) }
            submit(text)
        }
        override fun onError(message: String, fatal: Boolean) {
            _ui.update { it.copy(listening = false, partial = "", level = 0f, notice = message) }
        }
    })

    init {
        if (!prefs.saveHistory) viewModelScope.launch { dao.clear() }
        // Recover interrupted purchases and re-check what Myket says this account owns (silent if Myket is absent).
        viewModelScope.launch { sub.restore(); sub.clearNotice() }
    }

    fun setNotice(text: String?) = _ui.update { it.copy(notice = text) }

    fun toggleListening() {
        if (_ui.value.listening) { speech.stop() } else { tts.stop(); startListening() }
    }

    fun startListening() {
        if (_ui.value.listening) return
        _ui.update { it.copy(listening = true, notice = null, partial = "") }
        speech.start()
    }

    fun clearHistory() = viewModelScope.launch { dao.clear() }

    fun submit(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            dao.insert(ChatMessage(role = "user", text = t))
            val reply = process(t)
            dao.insert(ChatMessage(role = "assistant", text = reply))
            _ui.update { it.copy(busy = false) }
            if (prefs.speakReplies) {
                tts.speak(reply) { if (prefs.continuous && sub.isPro()) startListening() }
            } else if (prefs.continuous && sub.isPro()) startListening()
        }
    }

    private val engine = com.sadra.ai.ai.AssistantEngine(app, prefs)

    private suspend fun process(text: String): String =
        engine.process(text, dao.recent(12).reversed()).message

    override fun onCleared() {
        speech.destroy()
        tts.shutdown()
    }
}
