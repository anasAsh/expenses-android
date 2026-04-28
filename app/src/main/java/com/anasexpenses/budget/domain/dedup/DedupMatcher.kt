package com.anasexpenses.budget.domain.dedup

import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.domain.PrdConstants
import com.anasexpenses.budget.domain.merchant.MerchantSimilarity
import com.anasexpenses.budget.domain.time.epochMillisFrom
import java.time.ZoneId
import kotlin.math.abs

/** PRD §4.2.2 — amount + card + merchant similarity + ±5 min (amount equality checked before calling). */
object DedupMatcher {
    fun isDuplicate(
        cardLast4: String,
        normalizedMerchant: String,
        instantMillis: Long,
        existing: TransactionEntity,
        zone: ZoneId,
    ): Boolean {
        if (!cardsMatch(cardLast4, existing.cardLast4)) return false
        if (!MerchantSimilarity.isLikelyDuplicate(normalizedMerchant, existing.normalizedMerchant, PrdConstants.DEDUP_MERCHANT_SIMILARITY)) {
            return false
        }
        val existingInstant = epochMillisFrom(existing.dateEpochDay, existing.timeSecondOfDay, zone)
        return abs(instantMillis - existingInstant) <= PrdConstants.DEDUP_WINDOW_MS
    }

    private fun cardsMatch(candidate: String?, existing: String?): Boolean =
        when {
            candidate.isNullOrEmpty() && existing.isNullOrEmpty() -> true
            candidate.isNullOrEmpty() || existing.isNullOrEmpty() -> false
            else -> candidate == existing
        }
}
