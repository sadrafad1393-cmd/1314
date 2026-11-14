package com.sadra.ai.devicecontrol

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.sadra.ai.ai.Command
import com.sadra.ai.ai.MediaAction
import java.time.LocalTime

data class ExecResult(val ok: Boolean, val message: String)

/** Executes only allowlisted [Command]s using official Intents / AudioManager. No shell, no root, no accessibility hacks. */
class DeviceController(context: Context) {
    private val ctx = context.applicationContext
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Best-known package names; if one is missing we fall back to matching installed launcher labels.
    private val knownApps = mapOf(
        "روبیکا" to listOf("ir.resaneh1.iptv"),
        "تلگرام" to listOf("org.telegram.messenger", "org.telegram.messenger.web"),
        "کروم" to listOf("com.android.chrome"),
        "ایتا" to listOf("ir.eitaa.messenger"),
        "بله" to listOf("ir.nasim"),
        "اینستاگرام" to listOf("com.instagram.android"),
        "واتساپ" to listOf("com.whatsapp"),
        "یوتیوب" to listOf("com.google.android.youtube"),
        "نقشه" to listOf("com.google.android.apps.maps"),
        "بازار" to listOf("com.farsitel.bazaar")
    )

    fun execute(cmd: Command): ExecResult = try {
        when (cmd) {
            is Command.OpenApp -> openApp(cmd.appName)
            Command.OpenSettings -> launch(Intent(Settings.ACTION_SETTINGS), "تنظیمات باز شد.")
            Command.OpenWifiSettings -> launch(Intent(Settings.ACTION_WIFI_SETTINGS), "تنظیمات وای‌فای باز شد. (اندروید اجازه روشن/خاموش کردن مستقیم را به برنامه‌ها نمی‌دهد.)")
            is Command.SetAlarm -> setAlarm(cmd)
            is Command.SetTimer -> setTimer(cmd.seconds)
            is Command.Volume -> volume(cmd)
            is Command.Media -> media(cmd)
            Command.CurrentTime -> {
                val n = LocalTime.now()
                ExecResult(true, "ساعت الان ${n.hour}:${"%02d".format(n.minute)} است.")
            }
            is Command.WebSearch -> launch(
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, cmd.query),
                "در حال جستجو برای «${cmd.query}»."
            )
        }
    } catch (e: SecurityException) {
        ExecResult(false, "اجازه‌ی لازم برای این کار داده نشده است.")
    }

    private fun launch(intent: Intent, okMsg: String): ExecResult = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        ExecResult(true, okMsg)
    } catch (e: ActivityNotFoundException) {
        ExecResult(false, "برنامه‌ای برای انجام این کار روی گوشی پیدا نشد.")
    }

    private fun openApp(name: String): ExecResult {
        val pm = ctx.packageManager
        when (name) {
            "دوربین" -> return launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "دوربین باز شد.")
            "ماشین حساب" -> return launch(
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR),
                "ماشین حساب باز شد."
            )
            "تنظیمات" -> return launch(Intent(Settings.ACTION_SETTINGS), "تنظیمات باز شد.")
        }
        knownApps[name]?.forEach { pkg ->
            pm.getLaunchIntentForPackage(pkg)?.let { return launch(it, "$name باز شد.") }
        }
        // Fallback: match installed launcher app labels.
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(main, 0).firstOrNull {
            it.loadLabel(pm).toString().contains(name, ignoreCase = true)
        }
        val intent = match?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
        return if (intent != null) launch(intent, "$name باز شد.")
        else ExecResult(false, "برنامه‌ی «$name» روی گوشی پیدا نشد.")
    }

    private fun setAlarm(c: Command.SetAlarm): ExecResult {
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, c.hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, c.minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, c.label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return launch(i, "آلارم برای ساعت ${c.hour}:${"%02d".format(c.minute)} تنظیم شد.")
    }

    private fun setTimer(seconds: Int): ExecResult {
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        val label = if (seconds % 60 == 0) "${seconds / 60} دقیقه" else "$seconds ثانیه"
        return launch(i, "تایمر $label شروع شد.")
    }

    private fun volume(cmd: Command.Volume): ExecResult {
        val stream = AudioManager.STREAM_MUSIC
        val percent = cmd.percent
        if (percent != null) {
            val max = audio.getStreamMaxVolume(stream)
            val target = ((max * percent) / 100f).toInt().coerceIn(0, max)
            audio.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
            return ExecResult(true, "صدای رسانه روی $percent درصد تنظیم شد.")
        }
        val direction = cmd.up ?: return ExecResult(false, "میزان صدا مشخص نیست.")
        repeat(2) {
            audio.adjustStreamVolume(
                stream,
                if (direction) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                if (it == 1) AudioManager.FLAG_SHOW_UI else 0
            )
        }
        return ExecResult(true, if (direction) "صدا زیاد شد." else "صدا کم شد.")
    }

    private fun media(c: Command.Media): ExecResult {
        if (c.action == MediaAction.PLAY && c.query != null) {
            val i = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .putExtra(SearchManager.QUERY, c.query)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            val r = launch(i, "در حال پخش «${c.query}» با برنامه‌ی موسیقی شما.")
            if (r.ok) return r
        }
        val key = when (c.action) {
            MediaAction.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaAction.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
        return ExecResult(
            true,
            when (c.action) {
                MediaAction.PLAY -> "دستور پخش ارسال شد. اگر چیزی پخش نشد، یک برنامه‌ی موسیقی را یک بار باز کنید."
                MediaAction.PAUSE -> "پخش متوقف شد."
                MediaAction.NEXT -> "آهنگ بعدی."
                MediaAction.PREVIOUS -> "آهنگ قبلی."
            }
        )
    }
}
