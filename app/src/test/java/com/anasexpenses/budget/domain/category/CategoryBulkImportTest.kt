package com.anasexpenses.budget.domain.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBulkImportTest {
    @Test
    fun parses_example_block() {
        val text =
            """
            Gas: 100
            Eating Out: 200
            Mom: 250
            """.trimIndent()
        val r = CategoryBulkImport.parseLines(text)
        assertTrue(r.lineErrors.isEmpty())
        assertEquals(3, r.lines.size)
        assertEquals("Gas", r.lines[0].name)
        assertEquals(100_000L, r.lines[0].milliJod)
        assertEquals("Eating Out", r.lines[1].name)
        assertEquals(200_000L, r.lines[1].milliJod)
    }

    @Test
    fun skips_blank_lines() {
        val r = CategoryBulkImport.parseLines("A: 1\n\nB: 2\n")
        assertEquals(2, r.lines.size)
    }

    @Test
    fun rejects_bad_amount() {
        val r = CategoryBulkImport.parseLines("X: abc")
        assertTrue(r.lines.isEmpty())
        assertEquals(1, r.lineErrors.size)
    }
}
