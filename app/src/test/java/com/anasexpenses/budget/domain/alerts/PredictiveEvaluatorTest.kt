package com.anasexpenses.budget.domain.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveEvaluatorTest {
    @Test
    fun projected_linearInterpolation_midMonth() {
        val projected = PredictiveEvaluator.projectedMonthEndSpend(
            currentSpendMilli = 1000L * 1000L,
            dayOfMonth = 15,
            daysInMonth = 30,
        )
        assertEquals(2000L * 1000L, projected)
    }

    @Test
    fun predictive110_vs_target() {
        assertTrue(
            PredictiveEvaluator.exceedsPredictiveThreshold(
                projectedMilli = 1100,
                targetMilli = 1000,
            ),
        )
        assertFalse(
            PredictiveEvaluator.exceedsPredictiveThreshold(
                projectedMilli = 1099,
                targetMilli = 1000,
            ),
        )
    }
}
