package com.anasexpenses.budget.domain.merchant

/** PRD §4.2: merchant display + rule token (e.g. TALABAT.COM → talabat). */
object MerchantNormalizer {
    fun normalizedMerchant(raw: String): String =
        raw.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * Canonical token for [RuleEntity.merchant_token] — lowercase alnum only.
     * Strips common domain suffixes before stripping punctuation so `TALABAT.COM` → `talabat`.
     */
    fun merchantToken(raw: String): String {
        var s = raw.lowercase().trim()
        s = s.replace(Regex("\\.(com|net|jo|org)\\b"), "")
        return s.replace(Regex("[^a-z0-9]"), "").ifEmpty { "unknown" }
    }
}
