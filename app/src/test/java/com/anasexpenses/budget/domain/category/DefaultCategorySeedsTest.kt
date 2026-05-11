package com.anasexpenses.budget.domain.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategorySeedsTest {

    @Test
    fun rows_nonEmpty_uniqueNames_positiveTargets() {
        val rows = DefaultCategorySeeds.rows
        assertTrue(rows.isNotEmpty())
        assertEquals(rows.size, rows.map { it.name }.distinct().size)
        for (row in rows) {
            assertTrue(row.name.isNotBlank())
            assertTrue("${row.name} target", row.monthlyTargetMilliJod > 0L)
        }
    }
}
