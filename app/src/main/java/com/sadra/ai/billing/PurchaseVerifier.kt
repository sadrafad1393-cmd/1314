package com.sadra.ai.billing

import com.sadra.ai.settings.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Asks YOUR backend (POST <backend>/verify-purchase) to verify a purchase. */
class PurchaseVerifier(private val prefs: Prefs) {
    sealed interface Result {
        data class Verified(val expiresAt: Long) : Result
        data object Rejected : Result   // server says definitively invalid -> drop it
        data object Retry : Result      // outage / misconfig / no backend yet -> keep it and retry later
    }

    suspend fun verify(p: Purchase): Result = withContext(Dispatchers.IO) {
        val base = prefs.backendUrl.trimEnd('/')
        if (!base.startsWith("https://")) return@withContext Result.Retry
        try {
            val body = JSONObject().put("sku", p.productId).put("purchaseToken", p.token)
                .put("purchaseData", p.data).put("signature", p.signature).toString()
            val conn = (URL("$base/verify-purchase").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 20_000; doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (prefs.appToken.isNotBlank()) setRequestProperty("X-App-Token", prefs.appToken)
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            when (val code = conn.responseCode) {
                200 -> {
                    val j = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    if (j.optBoolean("ok") && j.optLong("expiresAt") > 0) Result.Verified(j.getLong("expiresAt")) else Result.Retry
                }
                403 -> { conn.errorStream?.close(); Result.Rejected }
                else -> { conn.errorStream?.close(); Result.Retry } // 401/5xx/etc: never discard a paid purchase on ambiguity
            }
        } catch (e: IOException) { Result.Retry } catch (e: Exception) { Result.Retry }
    }
}
