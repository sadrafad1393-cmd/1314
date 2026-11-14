package com.sadra.ai.network

import com.sadra.ai.ai.Command
import com.sadra.ai.memory.ChatMessage
import com.sadra.ai.settings.Prefs

/**
 * Deliberately does not contact a server or a hosted AI provider.
 *
 * The supplied project ZIP did not contain an on-device LLM runtime or model.
 * Until one is integrated and installed, free-form chat is reported as unavailable
 * rather than silently uploading conversation history or fabricating an AI answer.
 */
class AiClient(@Suppress("UNUSED_PARAMETER") prefs: Prefs) {
    sealed interface Result {
        data class Ok(val reply: String, val command: Command?) : Result
        data class Err(val message: String) : Result
    }

    suspend fun chat(@Suppress("UNUSED_PARAMETER") history: List<ChatMessage>): Result =
        Result.Err(
            "مدل هوش مصنوعی محلی هنوز نصب نشده است. در این نسخه، دستورهای پشتیبانی‌شده مثل باز کردن برنامه‌ها، تنظیم صدا، آلارم، تایمر و کنترل رسانه به‌صورت آفلاین اجرا می‌شوند؛ برای گفت‌وگوی آزاد باید موتور و مدل زبانی روی خود گوشی نصب شود. هیچ گفتگویی به سرور ارسال نشد."
        )
}
