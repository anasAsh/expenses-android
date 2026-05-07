package com.anasexpenses.budget.domain.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Converts 3-decimal milli units from a source currency into milli-JOD.
 *
 * Rates are static and intentionally conservative for offline parsing.
 */
object CurrencyToJodConverter {
    private val ratesToJodPerUnit: Map<String, BigDecimal> = mapOf(
        "JOD" to BigDecimal("1.000"),
        "USD" to BigDecimal("0.709"),
        "EUR" to BigDecimal("0.770"),
        "GBP" to BigDecimal("0.900"),
        "SAR" to BigDecimal("0.189"),
        "AED" to BigDecimal("0.193"),
        "QAR" to BigDecimal("0.195"),
        "KWD" to BigDecimal("2.304"),
        "BHD" to BigDecimal("1.880"),
        "OMR" to BigDecimal("1.840"),
    )

    fun toMilliJod(amountMilliInSourceCurrency: Long, sourceCurrency: String): Long? {
        val code = sourceCurrency.trim().uppercase(Locale.ROOT)
        val rate = ratesToJodPerUnit[code] ?: return null
        return BigDecimal.valueOf(amountMilliInSourceCurrency)
            .multiply(rate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}
