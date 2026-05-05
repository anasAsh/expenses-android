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

    /** May budget: Apr 25 – May 24 when salary/anchor is the 25th. */
    @Test
    fun may_labeled_day25_is_apr25_through_may24() {
        val ym = YearMonth.of(2026, 5)
        val r = BudgetCycle.epochDayRangeInclusive(ym, 25)
        assertEquals(LocalDate.of(2026, 4, 25).toEpochDay(), r.first)
        assertEquals(LocalDate.of(2026, 5, 24).toEpochDay(), r.last)
    }

    @Test
    fun april_labeled_day15_is_mar15_through_apr14() {
        val ym = YearMonth.of(2026, 4)
        val r = BudgetCycle.epochDayRangeInclusive(ym, 15)
        assertEquals(LocalDate.of(2026, 3, 15).toEpochDay(), r.first)
        assertEquals(LocalDate.of(2026, 4, 14).toEpochDay(), r.last)
    }

    @Test
    fun april25_day25_maps_to_labeled_may() {
        val d = LocalDate.of(2026, 4, 25)
        assertEquals(YearMonth.of(2026, 5), BudgetCycle.labeledYearMonthForDate(d, 25))
    }

    @Test
    fun may24_day25_maps_to_labeled_may() {
        val d = LocalDate.of(2026, 5, 24)
        assertEquals(YearMonth.of(2026, 5), BudgetCycle.labeledYearMonthForDate(d, 25))
    }

    @Test
    fun may25_day25_maps_to_labeled_june() {
        val d = LocalDate.of(2026, 5, 25)
        assertEquals(YearMonth.of(2026, 6), BudgetCycle.labeledYearMonthForDate(d, 25))
    }

    @Test
    fun march10_withStart15_is_labeled_march() {
        val d = LocalDate.of(2026, 3, 10)
        assertEquals(YearMonth.of(2026, 3), BudgetCycle.labeledYearMonthForDate(d, 15))
    }

    @Test
    fun april10_withStart15_is_labeled_april() {
        val d = LocalDate.of(2026, 4, 10)
        assertEquals(YearMonth.of(2026, 4), BudgetCycle.labeledYearMonthForDate(d, 15))
    }

    @Test
    fun april16_withStart15_is_labeled_may() {
        val d = LocalDate.of(2026, 4, 16)
        assertEquals(YearMonth.of(2026, 5), BudgetCycle.labeledYearMonthForDate(d, 15))
    }
}
