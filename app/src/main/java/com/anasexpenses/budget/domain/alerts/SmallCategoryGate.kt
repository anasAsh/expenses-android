package com.anasexpenses.budget.domain.alerts

/** PRD §4.7 — suppress push if target &lt; 5% of total budget OR &lt; 30 JOD (milli). */
object SmallCategoryGate {
    private const val THIRTY_JOD_MILLI = 30L * 1000L

    fun shouldSuppressPush(monthlyTargetMilliJod: Long, totalBudgetMilliJod: Long): Boolean {
        if (monthlyTargetMilliJod <= 0L) return true
        if (monthlyTargetMilliJod < THIRTY_JOD_MILLI) return true
        if (totalBudgetMilliJod <= 0L) return false
        val ratio = monthlyTargetMilliJod.toDouble() / totalBudgetMilliJod.toDouble()
        return ratio < 0.05
    }
}
