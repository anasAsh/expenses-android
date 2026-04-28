package com.anasexpenses.budget.domain.money

import java.math.BigDecimal
import java.math.RoundingMode

/** Store amounts as thousandths of JOD (3 decimal places). */
object JodMoney {
    fun parseToMilliJod(amountText: String): Long {
        val bd = BigDecimal(amountText.trim()).setScale(3, RoundingMode.UNNECESSARY)
        return bd.multiply(BigDecimal(1000)).longValueExact()
    }
}
