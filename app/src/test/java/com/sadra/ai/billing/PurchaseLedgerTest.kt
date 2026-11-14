package com.sadra.ai.billing

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PurchaseLedgerTest {
    private fun purchase(token: String, time: Long = System.currentTimeMillis()): Purchase {
        val d = JSONObject().put("productId", Plans.PRO_SKU).put("purchaseToken", token)
            .put("purchaseTime", time).put("purchaseState", 0).toString()
        return Purchase.parse(d, "sig")!!
    }

    @Test fun pendingPurchaseGivesProUntilItsOwnExpiryAndSurvivesRestart() {
        val store = MemoryStore()
        val now = System.currentTimeMillis()
        assertTrue(PurchaseLedger(store).addPending(purchase("t1", now)))
        val reopened = PurchaseLedger(store)                       // simulates app being killed and restarted
        assertEquals(1, reopened.pending().size)
        assertEquals(now + Plans.PRO_MS, reopened.proUntil())
    }

    @Test fun duplicateTokenIsRejected() {
        val l = PurchaseLedger(MemoryStore())
        assertTrue(l.addPending(purchase("t1")))
        assertFalse(l.addPending(purchase("t1")))
        assertEquals(1, l.pending().size)
    }

    @Test fun verifiedPurchaseClearsPendingAndCannotBeAddedAgain() {
        val l = PurchaseLedger(MemoryStore())
        val p = purchase("t1")
        l.addPending(p)
        l.markVerified("t1", p.time + Plans.PRO_MS)
        assertTrue(l.pending().isEmpty())
        assertFalse(l.addPending(p))
        assertEquals(p.time + Plans.PRO_MS, l.proUntil())
    }

    @Test fun removingRejectedPurchaseRemovesProAccess() {
        val l = PurchaseLedger(MemoryStore())
        l.addPending(purchase("t1"))
        l.removePending("t1")
        assertEquals(0L, l.proUntil())
    }

    @Test fun expiryNeverShrinksWhenOlderPurchaseVerifiedLater() {
        val l = PurchaseLedger(MemoryStore())
        l.markVerified("a", 2_000L); l.markVerified("b", 1_000L)
        assertEquals(2_000L, l.verifiedUntil())
    }
}
