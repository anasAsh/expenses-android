package com.anasexpenses.budget.domain

/** Frozen PRD §12 values for parsing and deduplication. */
object PrdConstants {
    const val CONFIDENCE_AUTO_MIN = 0.85f
    const val DEDUP_MERCHANT_SIMILARITY = 0.9f
    const val DEDUP_WINDOW_MS = 5 * 60 * 1000L
}
