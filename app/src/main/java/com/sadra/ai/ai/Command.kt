package com.sadra.ai.ai

/** Allowlisted actions. Nothing outside this set can ever be executed, whether it comes from the local parser or from the AI backend. */
sealed interface Command {
    data class OpenApp(val appName: String) : Command
    data object OpenSettings : Command
    data object OpenWifiSettings : Command
    data class SetAlarm(val hour: Int, val minute: Int, val label: String = "SADRA") : Command
    data class SetTimer(val seconds: Int) : Command
    data class Volume(val up: Boolean? = null, val percent: Int? = null) : Command
    data class Media(val action: MediaAction, val query: String? = null) : Command
    data object CurrentTime : Command
    data class WebSearch(val query: String) : Command
}

enum class MediaAction { PLAY, PAUSE, NEXT, PREVIOUS }
