package com.sadra.ai.billing

class MemoryStore : KeyValueStore {
    private val m = HashMap<String, Any?>()
    override fun getString(key: String) = m[key] as String?
    override fun putString(key: String, value: String?) { m[key] = value }
    override fun getLong(key: String, def: Long) = (m[key] as Long?) ?: def
    override fun putLong(key: String, value: Long) { m[key] = value }
}
