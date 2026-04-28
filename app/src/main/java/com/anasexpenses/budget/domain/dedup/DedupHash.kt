package com.anasexpenses.budget.domain.dedup

import java.security.MessageDigest

/** Stable key for linking pending/settled rows (PRD §4.2). Store lowercase hex in [TransactionEntity.dedup_hash]. */
object DedupHash {
    fun hash(cardLast4: String?, amountMilliJod: Long, instantEpochMillis: Long, merchantToken: String): String {
        val key = listOf(cardLast4 ?: "", amountMilliJod, instantEpochMillis, merchantToken.lowercase())
            .joinToString("|")
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(key.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
