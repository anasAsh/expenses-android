package com.anasexpenses.budget.domain.merchant

/** PRD §4.2.2 — similarity ≥ 0.9 for duplicate detection (normalized merchant strings). */
object MerchantSimilarity {
    fun score(a: String, b: String): Float {
        val x = a.lowercase()
        val y = b.lowercase()
        if (x == y) return 1f
        val maxLen = maxOf(x.length, y.length)
        if (maxLen == 0) return 1f
        val dist = levenshtein(x, y)
        return 1f - dist.toFloat() / maxLen.toFloat()
    }

    fun isLikelyDuplicate(a: String, b: String, threshold: Float = 0.9f): Boolean =
        score(a, b) >= threshold

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost,
                )
            }
        }
        return dp[m][n]
    }
}
