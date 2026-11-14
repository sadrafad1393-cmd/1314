package com.sadra.ai.ai

/** Offline rule-based parser for common Persian commands. Returns null when the sentence should go to the AI backend. */
object CommandParser {

    private val alarmKeys = listOf("بیدار", "آلارم", "زنگ", "هشدار", "یادم")
    private val moodWords = listOf("شاد", "آرام", "غمگین", "راک", "پاپ", "سنتی", "ریلکس", "آرامش")
    private val searchStop = setOf("درباره", "برام", "برای", "من", "سرچ", "جستجو", "کن", "کنید", "رو", "را", "لطفا", "یه", "کنم", "بکن", "درمورد", "در", "مورد", "تو", "گوگل")
    private val appStop = setOf("رو", "را", "لطفا", "برنامه", "اپ", "اپلیکیشن", "نرم", "افزار", "برام", "یه", "تو", "به", "برو")
    private val musicStop = setOf("یه", "یک", "پخش", "کن", "رو", "را", "برام", "لطفا", "بزن")

    fun parse(raw: String): Command? {
        val text = PersianText.normalize(raw)
        if (text.isEmpty()) return null
        val t = text.split(" ")
        val has = { s: String -> text.contains(s) }

        // Timer
        if (has("تایمر") || has("تایم شمار")) parseTimer(t)?.let { return it }

        // Alarm / reminder
        if (alarmKeys.any(has) && t.contains("ساعت")) parseAlarm(t, text, raw)?.let { return it }

        // Current time
        if (t.contains("ساعت") && (has("چنده") || has("چند ") || text.endsWith("چند"))) return Command.CurrentTime

        // Settings
        if (has("وای فای") || has("وایفای") || has("wifi") || has("wi-fi")) return Command.OpenWifiSettings
        if (has("تنظیمات") && (has("باز") || has("برو") || has("اجرا"))) return Command.OpenSettings

        // Volume: exact percentage or relative adjustment (offline).
        if (has("صدا") || has("ولوم")) {
            val percentIndex = t.indexOfFirst { it == "درصد" || it == "درصدی" }
            if (percentIndex > 0) {
                val number = PersianText.parseNumber(t, percentIndex - 1)?.first
                if (number != null && number in 0..100) return Command.Volume(percent = number)
            }
            if (t.contains("زیاد") || t.contains("بالا") || has("بلند")) return Command.Volume(up = true)
            if (t.contains("کم") || t.contains("پایین") || has("آهسته")) return Command.Volume(up = false)
        }

        // Media
        val music = has("آهنگ") || has("موسیقی") || has("میوزیک") || has("ترانه")
        if (has("بعدی")) return Command.Media(MediaAction.NEXT)
        if (has("قبلی")) return Command.Media(MediaAction.PREVIOUS)
        if ((has("متوقف") || has("قطع") || has("استپ") || has("ایست") || has("پاز")) && (music || has("پخش"))) {
            return Command.Media(MediaAction.PAUSE)
        }
        if (music && has("پخش")) {
            val q = if (moodWords.any(has)) t.filterNot { it in musicStop }.joinToString(" ") else null
            return Command.Media(MediaAction.PLAY, q)
        }

        // Web search
        if (has("سرچ") || has("جستجو")) {
            val q = t.filterNot { it in searchStop }.joinToString(" ")
            if (q.isNotBlank()) return Command.WebSearch(q)
        }

        // Open app
        val idx = t.indexOfFirst { it == "باز" || it == "اجرا" }
        if (idx > 0) {
            val name = t.subList(0, idx).filterNot { it in appStop }.joinToString(" ")
            if (name.isNotBlank()) return Command.OpenApp(name)
        }
        return null
    }

    private fun parseAlarm(t: List<String>, text: String, raw: String): Command? {
        val idx = t.indexOf("ساعت")
        val (h0, next) = PersianText.parseNumber(t, idx + 1) ?: return null
        var h = h0
        var m = 0
        if (next + 1 < t.size && t[next] == "و") {
            when (t[next + 1]) {
                "نیم" -> m = 30
                "ربع" -> m = 15
                else -> PersianText.parseNumber(t, next + 1)?.let { m = it.first }
            }
        }
        val padded = " $text "
        val pm = listOf(" عصر ", " شب ", " بعد از ظهر ", " بعدازظهر ").any { padded.contains(it) }
        if (pm && h in 1..11) h += 12
        if (h > 23 || m > 59) return null
        val label = if (text.contains("یادم")) raw.trim().take(60) else "SADRA"
        return Command.SetAlarm(h, m, label)
    }

    private fun parseTimer(t: List<String>): Command? {
        val units = mapOf("ثانیه" to 1, "دقیقه" to 60, "ساعت" to 3600)
        for (i in t.indices) {
            val unit = units[t[i]] ?: continue
            if (i > 0 && t[i - 1] == "نیم" && unit == 3600) return Command.SetTimer(1800)
            for (j in maxOf(0, i - 3) until i) {
                val r = PersianText.parseNumber(t, j)
                if (r != null && r.second == i && r.first > 0) return Command.SetTimer(r.first * unit)
            }
        }
        return null
    }
}
