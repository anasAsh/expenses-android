package com.anasexpenses.budget.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyToJodConverterTest {

    @Test
    fun toMilliJod_jod_passthrough() {
        assertEquals(12_410L, CurrencyToJodConverter.toMilliJod(12_410L, "JOD"))
    }

    @Test
    fun toMilliJod_usd_convertsWithStaticRate() {
        assertEquals(7_445L, CurrencyToJodConverter.toMilliJod(10_500L, "USD"))
    }

    @Test
    fun toMilliJod_unsupportedCurrency_returnsNull() {
        assertNull(CurrencyToJodConverter.toMilliJod(10_000L, "XYZ"))
    }
}
