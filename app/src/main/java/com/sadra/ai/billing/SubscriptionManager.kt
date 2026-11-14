package com.sadra.ai.billing

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.sadra.ai.BuildConfig
import com.sadra.ai.settings.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AccessState(
    val isPro: Boolean,
    val proUntil: Long,
    val remaining: Int,
    val limit: Int,
    val pending: Int
) {
    val daysLeft: Int get() = if (!isPro) 0 else (((proUntil - System.currentTimeMillis()) + Plans.DAY_MS - 1) / Plans.DAY_MS).toInt().coerceAtLeast(1)
}

/** Single place that decides what the user may do (FREE quota / PRO) and runs the purchase lifecycle. */
class SubscriptionManager private constructor(context: Context) {
    private val ctx = context.applicationContext
    private val store = SharedPrefsStore(ctx, "sadra_sub")
    private val prefs = Prefs(ctx)
    val quota = QuotaManager(store)
    private val ledger = PurchaseLedger(store)
    private val billing = MyketBillingClient(ctx)
    private val verifier = PurchaseVerifier(prefs)
    private val mutex = Mutex()

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<AccessState> = _state.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()
    fun clearNotice() { _notice.value = null }

    private fun compute(): AccessState {
        val until = ledger.proUntil()
        return AccessState(until > System.currentTimeMillis(), until, quota.remaining(), quota.limit, ledger.pending().size)
    }

    /** Also handles the midnight reset: call whenever the UI comes to the foreground. */
    fun refreshState() { _state.value = compute() }

    fun canRunCommand(): Boolean { val s = compute(); _state.value = s; return s.isPro || s.remaining > 0 }
    fun isPro(): Boolean = compute().isPro

    /** Only SUCCESSFUL commands count, and PRO is never counted. */
    fun recordSuccess() { if (!compute().isPro) quota.recordSuccess(); refreshState() }

    // ---------------------------------------------------------------- purchase flow

    /** Step 1: ask Myket for the purchase screen. */
    suspend fun prepareBuy(): PendingIntent? {
        if (BuildConfig.MYKET_RSA_KEY.isBlank()) { _notice.value = "کلید عمومی مایکت در برنامه تنظیم نشده است."; return null }
        if (isPro()) { _notice.value = "اشتراک PRO شما فعال است."; return null }
        if (ledger.pending().isNotEmpty()) { processPending(); _notice.value = "خرید قبلی شما در حال تأیید است."; return null }
        val payload = UUID.randomUUID().toString()
        store.putString("buy_payload", payload)
        return when (val r = billing.buyIntent(Plans.PRO_SKU, payload)) {
            is MyketBillingClient.BuyResult.Launch -> r.intent
            MyketBillingClient.BuyResult.AlreadyOwned -> { restore(); null } // owned but not activated here -> restore it
            is MyketBillingClient.BuyResult.Error -> { _notice.value = r.message; null }
        }
    }

    /** Step 2: result of the Myket purchase screen. */
    suspend fun onPurchaseResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) { _notice.value = "خرید لغو شد."; return }
        val p = MyketBillingClient.parseResult(data)
        if (p == null) { _notice.value = "خرید انجام نشد."; return }
        val expected = store.getString("buy_payload")
        if (p.payload.isNotEmpty() && expected != null && p.payload != expected) { _notice.value = "خرید نامعتبر بود."; return }
        accept(p)
    }

    /** Restore previous purchases (also run silently at startup): re-reads what Myket says this account owns. */
    suspend fun restore() {
        val owned = billing.ownedPurchases()
        if (owned == null) { _notice.value = "اتصال به مایکت برقرار نشد."; processPending(); return }
        var found = false
        for (p in owned.filter { it.productId == Plans.PRO_SKU }) {
            val expired = p.time + Plans.PRO_MS <= System.currentTimeMillis()
            if (expired) { billing.consume(p.token); continue }   // frees the product so it can be bought again
            found = true
            accept(p)
        }
        processPending()
        if (!found && !isPro()) _notice.value = "خرید فعالی برای بازیابی پیدا نشد."
    }

    private suspend fun accept(p: Purchase) = mutex.withLock {
        if (p.productId != Plans.PRO_SKU || p.state != 0) return@withLock
        if (!SignatureVerifier.verify(BuildConfig.MYKET_RSA_KEY, p.data, p.signature)) {
            _notice.value = "امضای خرید معتبر نیست."; return@withLock
        }
        if (p.time + Plans.PRO_MS <= System.currentTimeMillis()) return@withLock
        if (ledger.isDone(p.token)) { refreshState(); return@withLock }      // duplicate: already activated
        ledger.addPending(p)                                                   // persisted BEFORE network (no loss)
        refreshState()
        verifyPendingLocked()
    }

    /** True when nothing is left pending. Safe to call repeatedly (startup, WorkManager retry, restore). */
    suspend fun processPending(): Boolean = mutex.withLock { verifyPendingLocked() }

    private suspend fun verifyPendingLocked(): Boolean {
        for (p in ledger.pending()) {
            when (val r = verifier.verify(p)) {
                is PurchaseVerifier.Result.Verified -> {
                    ledger.markVerified(p.token, r.expiresAt)
                    _notice.value = "اشتراک PRO فعال شد 🎉"
                }
                PurchaseVerifier.Result.Rejected -> ledger.removePending(p.token)
                PurchaseVerifier.Result.Retry -> {}
            }
        }
        refreshState()
        val left = ledger.pending().isNotEmpty()
        if (left) scheduleRetry() else cancelRetry()
        return !left
    }

    private fun scheduleRetry() {
        val req = OneTimeWorkRequestBuilder<VerifyPendingWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK, ExistingWorkPolicy.KEEP, req)
    }

    private fun cancelRetry() { WorkManager.getInstance(ctx).cancelUniqueWork(WORK) }

    companion object {
        private const val WORK = "verify_pending_purchases"
        @Volatile private var inst: SubscriptionManager? = null
        fun get(ctx: Context): SubscriptionManager =
            inst ?: synchronized(this) { inst ?: SubscriptionManager(ctx).also { inst = it } }
    }
}

/** Retries verification with exponential backoff until the server answers, even after the app was closed. */
class VerifyPendingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result =
        if (SubscriptionManager.get(applicationContext).processPending()) Result.success() else Result.retry()
}
