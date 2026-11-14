package com.sadra.ai.billing

import android.content.Context

interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, def: Long = 0L): Long
    fun putLong(key: String, value: Long)
}

/** commit() (synchronous) on purpose: a successful purchase must hit disk before we do anything else. */
class SharedPrefsStore(context: Context, name: String) : KeyValueStore {
    private val sp = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    override fun getString(key: String) = sp.getString(key, null)
    override fun putString(key: String, value: String?) { sp.edit().putString(key, value).commit() }
    override fun getLong(key: String, def: Long) = sp.getLong(key, def)
    override fun putLong(key: String, value: Long) { sp.edit().putLong(key, value).commit() }
}
