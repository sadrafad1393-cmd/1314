package com.sadra.ai.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Helpers for becoming the default digital assistant through the official system UI only. */
object AssistantRole {
    fun isDefault(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) return rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        }
        val s = Settings.Secure.getString(ctx.contentResolver, "assistant") ?: return false
        return s.startsWith(ctx.packageName + "/")
    }

    /** Role-request dialog on Android 10+, otherwise the classic default-apps screen. */
    fun requestIntent(ctx: Context): Intent {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
            }
        }
        return settingsIntent()
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
}
