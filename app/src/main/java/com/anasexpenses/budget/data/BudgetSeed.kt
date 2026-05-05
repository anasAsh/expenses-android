package com.anasexpenses.budget.data

import com.anasexpenses.budget.data.local.dao.BankTemplateDao
import com.anasexpenses.budget.data.local.entity.BankTemplateEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Seeds `BankTemplate` rows (v1: Arab Bank English — see repo `Budget_Tracker_PRD.md` §14). */
@Singleton
class BudgetSeed @Inject constructor(
    private val bankTemplateDao: BankTemplateDao,
) {
    suspend fun ensureArabBankEnglishTemplate() {
        if (bankTemplateDao.countByBankId(BANK_ID_ARAB) > 0) return
        bankTemplateDao.insert(
            BankTemplateEntity(
                bankId = BANK_ID_ARAB,
                language = "en",
                regexPattern = ARAB_BANK_TRX_EN_REGEX,
                version = 1,
            ),
        )
    }

    companion object {
        const val BANK_ID_ARAB = "arab_bank"

        /**
         * Capture groups: 1 card last4, 2 merchant, 3 amount, 4 date (dd-MMM-yyyy), 5 time (HH:mm).
         * Ignores trailing balance clause per PRD.
         * `(?i)` — bank copy sometimes changes casing; timezone suffix is optional / may vary (GMT±n).
         */
        const val ARAB_BANK_TRX_EN_REGEX =
            """(?i)A Trx using Card XXXX(\d{4}) from (.+?) for JOD ([\d.]+) on (\d{2}-[A-Za-z]{3}-\d{4}) at (\d{2}:\d{2})(?:\s+GMT[+-]\d+)?"""
    }
}
