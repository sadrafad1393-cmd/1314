package com.sadra.ai.settings

import com.sadra.ai.BuildConfig

import android.content.Context
import com.sadra.ai.BuildConfig

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("sadra_prefs", Context.MODE_PRIVATE)

    /** Optional HTTPS endpoint used only for Myket purchase verification, never for chat/AI. */
    var backendUrl: String
        get() = sp.getString("backend_url", null) ?: BuildConfig.BACKEND_URL
        set(v) = sp.edit().putString("backend_url", v.trim()).apply()

    var appToken: String
        get() = sp.getString("app_token", "") ?: ""
        set(v) = sp.edit().putString("app_token", v.trim()).apply()

    /** When false, history is wiped on every app start and never sent as context across sessions. */
    var saveHistory: Boolean
        get() = sp.getBoolean("save_history", true)
        set(v) = sp.edit().putBoolean("save_history", v).apply()

    var speakReplies: Boolean
        get() = sp.getBoolean("speak", true)
        set(v) = sp.edit().putBoolean("speak", v).apply()

    /** Continuous conversation: listen again after each spoken reply (only while the app is open). */
    var continuous: Boolean
        get() = sp.getBoolean("continuous", false)
        set(v) = sp.edit().putBoolean("continuous", v).apply()
}
