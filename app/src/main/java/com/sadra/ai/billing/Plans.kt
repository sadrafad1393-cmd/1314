package com.sadra.ai.billing

object Plans {
    const val FREE_DAILY_LIMIT = 50
    const val PRO_SKU = "sadra_pro_60d"      // create this product in the Myket developer panel (price 150,000 Toman)
    const val PRO_DAYS = 60
    const val PRO_PRICE_TOMAN = 150_000
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val PRO_MS = PRO_DAYS * DAY_MS
}
