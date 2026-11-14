package com.sadra.ai.ai

import android.content.Context
import com.sadra.ai.billing.Plans
import com.sadra.ai.billing.SubscriptionManager
import com.sadra.ai.devicecontrol.DeviceController
import com.sadra.ai.memory.ChatMessage
import com.sadra.ai.network.AiClient
import com.sadra.ai.settings.Prefs

/** Shared brain used by both the main app screen and the glass assistant overlay. Call from the main thread. */
class AssistantEngine(ctx: Context, prefs: Prefs) {
    private val device = DeviceController(ctx)
    private val client = AiClient(prefs)
    private val sub = SubscriptionManager.get(ctx)

    /** [success] = the command really worked; only those count against the FREE daily quota. */
    data class Outcome(val message: String, val dismiss: Boolean, val success: Boolean = true)

    suspend fun process(text: String, history: List<ChatMessage>): Outcome {
        if (!sub.canRunCommand()) {
            return Outcome(
                "سهمیه‌ی رایگان امروز (${Plans.FREE_DAILY_LIMIT} دستور) تمام شد. فردا دوباره شارژ می‌شود، یا با اشتراک PRO نامحدود استفاده کن.",
                false, success = false
            )
        }
        val out = run(text, history)
        if (out.success) sub.recordSuccess()
        return out
    }

    private suspend fun run(text: String, history: List<ChatMessage>): Outcome {
        CommandParser.parse(text)?.let { return execute(it, null) }
        // PRO advanced feature: long conversation memory (FREE keeps the last 4 messages as context)
        val ctxMessages = if (sub.isPro()) history else history.takeLast(4)
        return when (val r = client.chat(ctxMessages)) {
            is AiClient.Result.Err -> Outcome(r.message, false, success = false)
            is AiClient.Result.Ok -> {
                val cmd = r.command
                if (cmd != null) execute(cmd, r.reply)
                else Outcome(r.reply.ifBlank { "متوجه نشدم، می‌شود دوباره بگویید؟" }, false, success = r.reply.isNotBlank())
            }
        }
    }

    private fun execute(cmd: Command, aiReply: String?): Outcome {
        val e = device.execute(cmd)
        val msg = if (e.ok && !aiReply.isNullOrBlank()) aiReply else e.message
        val leavesOverlay = cmd is Command.OpenApp || cmd is Command.OpenSettings ||
            cmd is Command.OpenWifiSettings || cmd is Command.WebSearch || (cmd is Command.Media && cmd.query != null)
        return Outcome(msg, e.ok && leavesOverlay, success = e.ok)
    }
}
