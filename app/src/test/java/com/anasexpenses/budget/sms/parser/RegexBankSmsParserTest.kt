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

    @Test
    fun arabBankEnglishSms_withoutTimezoneSuffix_stillParses() {
        val raw =
            "A Trx using Card XXXX2222 from TEST MERCHANT for JOD 1.000 on 01-May-2026 at 10:00. Available balance is JOD 5."

        val outcome = RegexBankSmsParser.parse(BudgetSeed.ARAB_BANK_TRX_EN_REGEX, raw)

        assertTrue(outcome is ParseOutcome.Success)
        val f = (outcome as ParseOutcome.Success).fields
        assertEquals("2222", f.cardLast4)
        assertEquals("TEST MERCHANT", f.merchantRaw)
        assertEquals(1_000L, f.amountMilliJod)
    }

    @Test
    fun arabBankArabicClickCredit_parsesAmountCardAndUsesFallbackClockFields() {
        val raw =
            """
            JO
            تم قيد دفعة كليك بمبلغ 250.000 JOD من حساب 500*0189 الرصيد 3549.242 JOD
            """.trimIndent()

        val day = LocalDate.of(2026, 4, 29).toEpochDay()
        val sec = 14 * 3600 + 30 * 60
        val outcome = RegexBankSmsParser.parseArabClickCredit(raw, day, sec)

        assertTrue(outcome is ParseOutcome.Success)
        val success = outcome as ParseOutcome.Success
        assertEquals(0.82f, success.confidence, 0.001f)
        val f = success.fields
        assertEquals("0189", f.cardLast4)
        assertEquals("كليك", f.merchantRaw)
        assertEquals(250_000L, f.amountMilliJod)
        assertEquals("JOD", f.currency)
        assertEquals(day, f.dateEpochDay)
        assertEquals(sec, f.timeSecondOfDay)
    }

    @Test
    fun arabBankArabicClickCredit_withoutBalanceLine_parses() {
        val raw =
            "تم قيد دفعة كليك بمبلغ 100.500 JOD من حساب 500*4242 شكراً لاستخدامكم"

        val outcome = RegexBankSmsParser.parseArabClickCredit(raw)

        assertTrue(outcome is ParseOutcome.Success)
        val f = (outcome as ParseOutcome.Success).fields
        assertEquals("4242", f.cardLast4)
        assertEquals(100_500L, f.amountMilliJod)
    }

    @Test
    fun arabBankArabicClickCredit_userSampleExact_multiline_parses() {
        val raw =
            """
            JO
            تم قيد دفعة كليك بمبلغ 250.000 JOD من حساب 500*0189 الرصيد 3549.242 JOD
            """.trimIndent()

        val outcome = RegexBankSmsParser.parseArabClickCredit(raw)

        assertTrue(outcome is ParseOutcome.Success)
        val f = (outcome as ParseOutcome.Success).fields
        assertEquals("0189", f.cardLast4)
        assertEquals(250_000L, f.amountMilliJod)
    }

    @Test
    fun arabBankArabicClickCredit_loosePattern_extraTextBetweenChunks_parses() {
        val raw =
            "عرضنا لكم كليك\nبمبلغ 50.000 JOD — تنبيه\nمن حساب جاري 900*5566 نهاية"

        val outcome = RegexBankSmsParser.parseArabClickCredit(raw)

        assertTrue(outcome is ParseOutcome.Success)
        val f = (outcome as ParseOutcome.Success).fields
        assertEquals("5566", f.cardLast4)
        assertEquals(50_000L, f.amountMilliJod)
    }
}
