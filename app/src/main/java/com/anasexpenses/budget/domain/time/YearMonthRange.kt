package com.anasexpenses.budget.domain.time

import java.time.LocalDate
import java.time.YearMonth

object YearMonthRange {
    fun epochDayRangeInclusive(month: YearMonth): LongRange {
        val start = month.atDay(1).toEpochDay()
        val end = month.atEndOfMonth().toEpochDay()
        return start..end
    }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun currentMonth(): YearMonth = YearMonth.now()
}
