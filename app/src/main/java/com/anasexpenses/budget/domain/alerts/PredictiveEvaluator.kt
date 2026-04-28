package com.anasexpenses.budget.domain.alerts

/** PRD §4.7.2 — projected month-end vs target; alert when ≥ 110%. */
object PredictiveEvaluator {
    fun projectedMonthEndSpend(currentSpendMilli: Long, dayOfMonth: Int, daysInMonth: Int): Long {
        if (dayOfMonth <= 0 || daysInMonth <= 0) return currentSpendMilli
        return (currentSpendMilli * daysInMonth) / dayOfMonth
    }

    fun exceedsPredictiveThreshold(projectedMilli: Long, targetMilli: Long): Boolean {
        if (targetMilli <= 0L) return false
        return projectedMilli * 100L >= targetMilli * 110L
    }
}
