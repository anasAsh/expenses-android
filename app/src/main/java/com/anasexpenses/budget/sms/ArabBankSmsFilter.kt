package com.anasexpenses.budget.sms

/** Cheap body heuristic before regex parsing (reduces work on non–Arab Bank SMS). */
object ArabBankSmsFilter {
    private val cardMaskEnglish = Regex("""(?i)Card\s+XXXX\s*\d{4}""")
    private val amountWithCurrency = Regex("""(?i)for\s+[A-Z]{3}\s+[\d.]+""")

    fun likelyArabBankTrx(body: String): Boolean {
        val b = body.trim()
        if (b.contains("Trx using Card", ignoreCase = true)) return true
        // English variants without exact "Trx using Card" marketing wording
        if (cardMaskEnglish.containsMatchIn(b) && amountWithCurrency.containsMatchIn(b)) return true
        // Arabic Click — wording varies; parser uses multiple regexes.
        if (b.contains("كليك")) {
            if (b.contains("تم قيد دفعة كليك") || b.contains("دفعة كليك")) return true
            if (b.contains("بمبلغ") || b.contains("من حساب")) return true
        }
        return false
    }
}
