package com.anasexpenses.budget.domain.time

import java.time.LocalDate
import java.time.YearMonth

object YearMonthRange {
    /** Calendar month (1st–last day). Prefer [BudgetCycle.epochDayRangeInclusive] when using a custom cycle start day. */
    fun epochDayRangeInclusive(month: YearMonth): LongRange =
        BudgetCycle.epochDayRangeInclusive(month, BudgetCycle.MIN_START_DAY)

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun currentMonth(): YearMonth = YearMonth.now()
}
