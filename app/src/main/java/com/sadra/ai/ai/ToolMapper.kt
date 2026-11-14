package com.sadra.ai.ai

import org.json.JSONObject

/** Validates and maps tool calls returned by the AI backend onto the allowlisted [Command] set. Unknown tools are dropped. */
object ToolMapper {
    fun toCommand(name: String, args: JSONObject): Command? = when (name) {
        "open_app" -> args.optString("name").trim().takeIf { it.isNotEmpty() && it.length <= 40 }?.let { Command.OpenApp(it) }
        "open_settings" -> Command.OpenSettings
        "open_wifi_settings" -> Command.OpenWifiSettings
        "set_alarm" -> {
            val h = args.optInt("hour", -1)
            val m = args.optInt("minute", 0)
            if (h in 0..23 && m in 0..59) Command.SetAlarm(h, m, args.optString("label", "SADRA").take(60)) else null
        }
        "set_timer" -> args.optInt("seconds", -1).takeIf { it in 1..86400 }?.let { Command.SetTimer(it) }
        "volume" -> when (args.optString("direction")) {
            "up" -> Command.Volume(true)
            "down" -> Command.Volume(false)
            else -> null
        }
        "media" -> when (args.optString("action")) {
            "play" -> Command.Media(MediaAction.PLAY, args.optString("query").takeIf { it.isNotBlank() }?.take(80))
            "pause" -> Command.Media(MediaAction.PAUSE)
            "next" -> Command.Media(MediaAction.NEXT)
            "previous" -> Command.Media(MediaAction.PREVIOUS)
            else -> null
        }
        "web_search" -> args.optString("query").trim().takeIf { it.isNotEmpty() }?.take(120)?.let { Command.WebSearch(it) }
        "current_time" -> Command.CurrentTime
        else -> null
    }
}
