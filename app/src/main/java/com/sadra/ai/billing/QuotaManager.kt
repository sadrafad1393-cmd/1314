package com.sadra.ai.billing

import java.time.LocalDate

/** FREE plan: N successful commands per local calendar day; resets automatically when the date changes. */
class QuotaManager(
    private val store: KeyValueStore,
    private val today: () -> String = { LocalDate.now().toString() },
    val limit: Int = Plans.FREE_DAILY_LIMIT
) {
    private fun used(): Int = if (store.getString(DAY) == today()) store.getLong(COUNT).toInt() else 0
    fun remaining(): Int = (limit - used()).coerceAtLeast(0)
    fun canUse(): Boolean = remaining() > 0
    fun recordSuccess() {
        val t = today()
        val c = if (store.getString(DAY) == t) store.getLong(COUNT) else 0L
        store.putString(DAY, t)
        store.putLong(COUNT, c + 1)
    }
    private companion object { const val DAY = "q_day"; const val COUNT = "q_count" }
}
