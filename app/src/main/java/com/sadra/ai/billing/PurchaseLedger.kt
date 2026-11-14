package com.sadra.ai.billing

import org.json.JSONArray
import org.json.JSONObject

data class Purchase(
    val productId: String,
    val token: String,
    val time: Long,
    val state: Int,
    val data: String,       // raw signed JSON from Myket
    val signature: String,
    val payload: String = ""
) {
    companion object {
        fun parse(data: String, signature: String): Purchase? = try {
            val j = JSONObject(data)
            Purchase(
                productId = j.getString("productId"),
                token = j.getString("purchaseToken"),
                time = j.getLong("purchaseTime"),
                state = j.optInt("purchaseState", 0),
                data = data, signature = signature,
                payload = j.optString("developerPayload", "")
            )
        } catch (e: Exception) { null }
    }
}

/**
 * Durable record of purchases. A purchase is written to `pending` BEFORE any network call, and only removed after the
 * server confirmed (or definitively rejected) it, so a crash / outage / killed app can never lose a paid purchase.
 */
class PurchaseLedger(private val store: KeyValueStore) {

    fun pending(): List<Purchase> = readArray(PENDING).mapNotNull { Purchase.parse(it.first, it.second) }

    /** Returns false if this token is already known (pending or done) -> duplicate transaction. */
    fun addPending(p: Purchase): Boolean {
        if (isDone(p.token) || pending().any { it.token == p.token }) return false
        val arr = JSONArray(store.getString(PENDING) ?: "[]")
        arr.put(JSONObject().put("d", p.data).put("s", p.signature))
        store.putString(PENDING, arr.toString())
        return true
    }

    fun removePending(token: String) {
        val kept = JSONArray()
        readArray(PENDING).filter { Purchase.parse(it.first, it.second)?.token != token }
            .forEach { kept.put(JSONObject().put("d", it.first).put("s", it.second)) }
        store.putString(PENDING, kept.toString())
    }

    fun isDone(token: String): Boolean = doneTokens().contains(token)

    fun markVerified(token: String, expiresAt: Long) {
        val arr = JSONArray(store.getString(DONE) ?: "[]")
        if (!doneTokens().contains(token)) arr.put(token)
        store.putString(DONE, arr.toString())
        if (expiresAt > verifiedUntil()) store.putLong(UNTIL, expiresAt)
        removePending(token)
    }

    fun verifiedUntil(): Long = store.getLong(UNTIL, 0L)

    /** PRO until: server-verified expiry, or (while verification is pending) the signature-checked purchase's own expiry. */
    fun proUntil(): Long = maxOf(verifiedUntil(), pending().maxOfOrNull { it.time + Plans.PRO_MS } ?: 0L)

    private fun doneTokens(): Set<String> {
        val a = JSONArray(store.getString(DONE) ?: "[]")
        return (0 until a.length()).map { a.getString(it) }.toSet()
    }

    private fun readArray(key: String): List<Pair<String, String>> {
        val a = JSONArray(store.getString(key) ?: "[]")
        return (0 until a.length()).map { a.getJSONObject(it).let { o -> o.getString("d") to o.getString("s") } }
    }

    private companion object { const val PENDING = "pending"; const val DONE = "done"; const val UNTIL = "verified_until" }
}
