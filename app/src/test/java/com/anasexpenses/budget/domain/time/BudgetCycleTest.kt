package com.anasexpenses.budget.domain.time

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCycleTest {
    @Test
    fun calendarMonth_day1_matches_april() {
        val ym = YearMonth.of(2026, 4)
        val r = BudgetCycle.epochDayRangeInclusive(ym, 1)
        assertEquals(LocalDate.of(2026, 4, 1).toEpochDay(), r.first)
        assertEquals(LocalDate.of(2026, 4, 30).toEpochDay(), r.last)
    }

    @Test
    fun day15_april_runs_to_may14() {
        val ym = YearMonth.of(2026, 4)
        val r = BudgetCycle.epochDayRangeInclusive(ym, 15)
        assertEquals(LocalDate.of(2026, 4, 15).toEpochDay(), r.first)
        assertEquals(LocalDate.of(2026, 5, 14).toEpochDay(), r.last)
    }

    @Test
    fun march10_withStart15_is_february_labeled_month() {
        val d = LocalDate.of(2026, 3, 10)
        assertEquals(YearMonth.of(2026, 2), BudgetCycle.labeledYearMonthForDate(d, 15))
    }

    @Test
    fun april10_withStart15_is_march_labeled_month() {
        val d = LocalDate.of(2026, 4, 10)
        assertEquals(YearMonth.of(2026, 3), BudgetCycle.labeledYearMonthForDate(d, 15))
    }

    @Test
    fun april16_withStart15_is_april_labeled_month() {
        val d = LocalDate.of(2026, 4, 16)
        assertEquals(YearMonth.of(2026, 4), BudgetCycle.labeledYearMonthForDate(d, 15))
    }

    @Test
    fun april20_withStart15_is_april_labeled_month() {
        val d = LocalDate.of(2026, 4, 20)
        assertEquals(YearMonth.of(2026, 4), BudgetCycle.labeledYearMonthForDate(d, 15))
    }
}
