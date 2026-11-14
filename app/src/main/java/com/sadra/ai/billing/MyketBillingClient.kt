package com.sadra.ai.billing

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.vending.billing.IInAppBillingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Thin client for the Myket In-App Billing service (IAB v3 compatible). Connects per call and always disconnects. */
class MyketBillingClient(context: Context) {
    private val ctx = context.applicationContext

    sealed interface BuyResult {
        data class Launch(val intent: PendingIntent) : BuyResult
        data object AlreadyOwned : BuyResult
        data class Error(val message: String) : BuyResult
    }

    private suspend fun <T> withService(block: (IInAppBillingService) -> T): T? {
        var conn: ServiceConnection? = null
        val svc: IInAppBillingService? = suspendCancellableCoroutine { cont ->
            val c = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, b: IBinder?) {
                    if (cont.isActive) cont.resume(IInAppBillingService.Stub.asInterface(b))
                }
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            conn = c
            val intent = Intent(BIND_ACTION).setPackage(MARKET_PACKAGE)
            val ok = try { ctx.bindService(intent, c, Context.BIND_AUTO_CREATE) } catch (e: SecurityException) { false }
            if (!ok && cont.isActive) cont.resume(null)
        }
        return try {
            if (svc == null) null else withContext(Dispatchers.IO) { try { block(svc) } catch (e: Exception) { null } }
        } finally {
            conn?.let { try { ctx.unbindService(it) } catch (_: Exception) {} }
        }
    }

    /** Owned, not-yet-consumed purchases of this account. null = Myket unavailable / error. */
    suspend fun ownedPurchases(): List<Purchase>? = withService { svc ->
        val out = mutableListOf<Purchase>()
        var token: String? = null
        do {
            val b = svc.getPurchases(API, ctx.packageName, "inapp", token)
            if (b.getInt("RESPONSE_CODE", -1) != 0) return@withService null
            val datas = b.getStringArrayList("INAPP_PURCHASE_DATA_LIST") ?: arrayListOf()
            val sigs = b.getStringArrayList("INAPP_DATA_SIGNATURE_LIST") ?: arrayListOf()
            datas.forEachIndexed { i, d -> Purchase.parse(d, sigs.getOrNull(i) ?: "")?.let(out::add) }
            token = b.getString("INAPP_CONTINUATION_TOKEN")
        } while (token != null)
        out
    }

    suspend fun buyIntent(sku: String, payload: String): BuyResult {
        val r = withService { svc ->
            val b = svc.getBuyIntent(API, ctx.packageName, sku, "inapp", payload)
            when (val code = b.getInt("RESPONSE_CODE", -1)) {
                0 -> {
                    @Suppress("DEPRECATION")
                    val pi = b.getParcelable<PendingIntent>("BUY_INTENT")
                    if (pi != null) BuyResult.Launch(pi) else BuyResult.Error("پاسخ نامعتبر از مایکت.")
                }
                7 -> BuyResult.AlreadyOwned
                else -> BuyResult.Error("خرید شروع نشد (کد $code).")
            }
        }
        return r ?: BuyResult.Error("مایکت روی این گوشی نصب نیست یا پاسخ نمی‌دهد.")
    }

    suspend fun consume(token: String): Boolean = withService { it.consumePurchase(API, ctx.packageName, token) == 0 } == true

    companion object {
        const val API = 3
        const val BIND_ACTION = "ir.mservices.market.InAppBillingService.BIND"
        const val MARKET_PACKAGE = "ir.mservices.market"

        /** Parses the Intent returned to onActivityResult after the Myket purchase screen. */
        fun parseResult(data: Intent?): Purchase? {
            if (data == null || data.getIntExtra("RESPONSE_CODE", -1) != 0) return null
            val d = data.getStringExtra("INAPP_PURCHASE_DATA") ?: return null
            return Purchase.parse(d, data.getStringExtra("INAPP_DATA_SIGNATURE") ?: "")
        }
    }
}
