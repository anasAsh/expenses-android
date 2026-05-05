package com.anasexpenses.budget.domain.time

import java.time.LocalDate
import java.time.YearMonth

/**
 * Budget periods keyed by [YearMonth] (`YYYY-MM`).
 *
 * **Calendar month** ([cycleStartDay] `1`): period is that full calendar month (1st–last day).
 *
 * **Pay cycle** ([cycleStartDay] `2`–`28`): the label is the **salary / budget month** that ends the day before
 * the next transfer. Example with day **25**: **May** runs **Apr 25 – May 24** (May cycle starts Apr 25,
 * ends May 24). Formula: start = anchor on **previous** calendar month, end = day before anchor on **labeled** month.
 *
 * Start days are clamped to [MIN_START_DAY]–[MAX_START_DAY].
 */
object BudgetCycle {
    const val MIN_START_DAY = 1
    const val MAX_START_DAY = 28

    fun clampStartDay(day: Int): Int = day.coerceIn(MIN_START_DAY, MAX_START_DAY)

    fun epochDayRangeInclusive(labeledMonth: YearMonth, cycleStartDay: Int): LongRange {
        val d = clampStartDay(cycleStartDay)
        if (d <= MIN_START_DAY) {
            val start = labeledMonth.atDay(1).toEpochDay()
            val end = labeledMonth.atEndOfMonth().toEpochDay()
            return start..end
        }
        val startDate = safeDay(labeledMonth.minusMonths(1), d)
        val endDate = safeDay(labeledMonth, d).minusDays(1)
        return startDate.toEpochDay()..endDate.toEpochDay()
    }

    private fun safeDay(ym: YearMonth, preferredDay: Int): LocalDate {
        val max = ym.lengthOfMonth()
        val day = minOf(preferredDay, max)
        return ym.atDay(day)
    }

    /**
     * Which labeled budget month [date] falls under for the given cycle start day.
     */
    fun labeledYearMonthForDate(date: LocalDate, cycleStartDay: Int): YearMonth {
        val d = clampStartDay(cycleStartDay)
        if (d <= MIN_START_DAY) {
            return YearMonth.from(date)
        }
        val epoch = date.toEpochDay()
        val ym = YearMonth.from(date)
        for (delta in longArrayOf(-1L, 0L, 1L)) {
            val candidate = ym.plusMonths(delta)
            val range = epochDayRangeInclusive(candidate, d)
            if (epoch in range) return candidate
        }
        return ym
    }

    fun daysInCycle(labeledMonth: YearMonth, cycleStartDay: Int): Int {
        val r = epochDayRangeInclusive(labeledMonth, cycleStartDay)
        return (r.last - r.first + 1).toInt().coerceAtLeast(1)
    }

    /** 1-based index of [date] within [labeledMonth]'s cycle, or null if outside that window. */
    fun dayOfCycle(date: LocalDate, labeledMonth: YearMonth, cycleStartDay: Int): Int? {
        val r = epochDayRangeInclusive(labeledMonth, cycleStartDay)
        val e = date.toEpochDay()
        if (e !in r) return null
        return (e - r.first + 1).toInt()
    }
}
