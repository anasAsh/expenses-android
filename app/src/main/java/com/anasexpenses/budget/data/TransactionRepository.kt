package com.anasexpenses.budget.data

import com.anasexpenses.budget.data.local.dao.BankTemplateDao
import com.anasexpenses.budget.data.local.dao.RuleDao
import com.anasexpenses.budget.data.local.dao.TransactionDao
import com.anasexpenses.budget.data.local.entity.BankTemplateEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxSource
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.domain.PrdConstants
import com.anasexpenses.budget.domain.dedup.DedupMatcher
import com.anasexpenses.budget.domain.merchant.MerchantNormalizer
import com.anasexpenses.budget.domain.time.epochMillisFrom
import com.anasexpenses.budget.sms.parser.ParseOutcome
import com.anasexpenses.budget.sms.parser.ParsedBankSms
import com.anasexpenses.budget.sms.parser.RegexBankSmsParser
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val ruleDao: RuleDao,
    private val bankTemplateDao: BankTemplateDao,
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    suspend fun ingestSmsBodies(rawBodies: List<String>) {
        val template = bankTemplateDao.getByBankAndLanguage(BudgetSeed.BANK_ID_ARAB, "en") ?: return
        for (body in rawBodies) {
            ingestArabBankSms(body, template)
        }
    }

    private suspend fun ingestArabBankSms(rawSms: String, template: BankTemplateEntity) {
        when (val outcome = RegexBankSmsParser.parse(template.regexPattern, rawSms)) {
            ParseOutcome.NoMatch -> return
            is ParseOutcome.Failure -> return
            is ParseOutcome.Success -> {
                val fields = outcome.fields
                if (!fields.currency.equals("JOD", ignoreCase = true)) return
                insertIfNotDuplicate(rawSms, template.id, fields, outcome.confidence)
            }
        }
    }

    private suspend fun insertIfNotDuplicate(
        rawSms: String,
        bankTemplateId: Long,
        fields: ParsedBankSms,
        confidence: Float,
    ) {
        val normalizedMerchant = MerchantNormalizer.normalizedMerchant(fields.merchantRaw)
        val token = MerchantNormalizer.merchantToken(fields.merchantRaw)
        val categoryId = ruleDao.getByMerchantToken(token)?.categoryId
        val status =
            if (confidence >= PrdConstants.CONFIDENCE_AUTO_MIN) TxStatus.AUTO else TxStatus.NEEDS_REVIEW
        val instantMillis = epochMillisFrom(fields.dateEpochDay, fields.timeSecondOfDay, zone)

        val candidates = transactionDao.findSameDayAndAmount(fields.amountMilliJod, fields.dateEpochDay)
        val duplicate = candidates.any { existing ->
            DedupMatcher.isDuplicate(
                cardLast4 = fields.cardLast4,
                normalizedMerchant = normalizedMerchant,
                instantMillis = instantMillis,
                existing = existing,
                zone = zone,
            )
        }
        if (duplicate) return

        val now = System.currentTimeMillis()
        transactionDao.insert(
            TransactionEntity(
                amountMilliJod = fields.amountMilliJod,
                currency = fields.currency,
                merchant = fields.merchantRaw,
                normalizedMerchant = normalizedMerchant,
                normalizedMerchantToken = token,
                categoryId = categoryId,
                dateEpochDay = fields.dateEpochDay,
                timeSecondOfDay = fields.timeSecondOfDay,
                source = TxSource.SMS,
                confidence = confidence,
                status = status,
                isRefund = false,
                rawSms = rawSms,
                cardLast4 = fields.cardLast4,
                dedupHash = null,
                bankTemplateId = bankTemplateId,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
    }
}
