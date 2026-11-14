package com.sadra.ai.billing

import org.junit.Assert.*
import org.junit.Test

class QuotaManagerTest {
    @Test fun startsWithFullQuota() = assertEquals(50, QuotaManager(MemoryStore()).remaining())

    @Test fun blocksAfter50SuccessfulCommands() {
        val q = QuotaManager(MemoryStore(), { "2026-11-14" })
        repeat(49) { q.recordSuccess() }
        assertTrue(q.canUse()); assertEquals(1, q.remaining())
        q.recordSuccess()
        assertFalse(q.canUse()); assertEquals(0, q.remaining())
    }

    @Test fun resetsAutomaticallyOnNewDay() {
        var day = "2026-11-14"
        val q = QuotaManager(MemoryStore(), { day })
        repeat(50) { q.recordSuccess() }
        assertFalse(q.canUse())
        day = "2026-11-15"
        assertEquals(50, q.remaining()); assertTrue(q.canUse())
        q.recordSuccess(); assertEquals(49, q.remaining())
    }

    @Test fun survivesRestartBecauseItIsPersisted() {
        val store = MemoryStore()
        QuotaManager(store, { "d" }).apply { repeat(7) { recordSuccess() } }
        assertEquals(43, QuotaManager(store, { "d" }).remaining())
    }
}
