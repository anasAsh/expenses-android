package com.anasexpenses.budget.sms

/** Cheap body heuristic before regex parsing (reduces work on non–Arab Bank SMS). */
object ArabBankSmsFilter {
    fun likelyArabBankTrx(body: String): Boolean {
        val b = body.trim()
        return b.contains("Trx using Card", ignoreCase = true) &&
            b.contains("JOD", ignoreCase = true)
    }
}
