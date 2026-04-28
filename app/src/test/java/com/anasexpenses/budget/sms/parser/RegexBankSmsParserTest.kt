package com.anasexpenses.budget.sms.parser

import com.anasexpenses.budget.data.BudgetSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RegexBankSmsParserTest {

    @Test
    fun arabBankGoldenSms_parsesPrdSample() {
        val raw =
            "A Trx using Card XXXX4259 from Talabat for JOD 12.410 on 04-Apr-2026 at 13:52 GMT+3. Available balance is JOD 6951.447."

        val outcome = RegexBankSmsParser.parse(BudgetSeed.ARAB_BANK_TRX_EN_REGEX, raw)

        assertTrue(outcome is ParseOutcome.Success)
        val success = outcome as ParseOutcome.Success
        assertEquals(0.95f, success.confidence, 0.001f)

        val f = success.fields
        assertEquals("4259", f.cardLast4)
        assertEquals("Talabat", f.merchantRaw)
        assertEquals(12_410L, f.amountMilliJod)
        assertEquals("JOD", f.currency)
        assertEquals(LocalDate.of(2026, 4, 4).toEpochDay(), f.dateEpochDay)
        assertEquals(13 * 3600 + 52 * 60, f.timeSecondOfDay)
    }

    @Test
    fun arabBankEnglishSmsSecondSample_parsesFields() {
        val raw =
            "A Trx using Card XXXX1111 from CARREFOUR AMMAN for JOD 55.000 on 28-Apr-2026 at 09:15 GMT+3. Available balance is JOD 1000.000."

        val outcome = RegexBankSmsParser.parse(BudgetSeed.ARAB_BANK_TRX_EN_REGEX, raw)

        assertTrue(outcome is ParseOutcome.Success)
        val success = outcome as ParseOutcome.Success
        val f = success.fields
        assertEquals("1111", f.cardLast4)
        assertEquals("CARREFOUR AMMAN", f.merchantRaw)
        assertEquals(55_000L, f.amountMilliJod)
        assertEquals(LocalDate.of(2026, 4, 28).toEpochDay(), f.dateEpochDay)
        assertEquals(9 * 3600 + 15 * 60, f.timeSecondOfDay)
    }
}
