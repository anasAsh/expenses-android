package com.anasexpenses.budget.domain.time

import java.time.LocalDate
import java.time.YearMonth

/**
 * Budget periods labeled by [YearMonth]: spending runs from [cycleStartDay] on that calendar month
 * through the day before the same numbered day next month (e.g. day 15 → 15th → 14th next month).
 * Start days are clamped to [MIN_START_DAY]–[MAX_START_DAY] so every month has a valid anchor.
 */
object BudgetCycle {
    const val MIN_START_DAY = 1
    const val MAX_START_DAY = 28

    fun clampStartDay(day: Int): Int = day.coerceIn(MIN_START_DAY, MAX_START_DAY)

    fun epochDayRangeInclusive(labeledMonth: YearMonth, cycleStartDay: Int): LongRange {
        val d = clampStartDay(cycleStartDay)
        val startDate = safeDay(labeledMonth, d)
        val endDate = safeDay(labeledMonth.plusMonths(1), d).minusDays(1)
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
