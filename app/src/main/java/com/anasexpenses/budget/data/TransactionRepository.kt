package com.anasexpenses.budget.data

import com.anasexpenses.budget.alerts.BudgetAlertCoordinator
import com.anasexpenses.budget.data.local.dao.BankTemplateDao
import com.anasexpenses.budget.data.local.dao.RuleDao
import com.anasexpenses.budget.data.local.dao.TransactionDao
import com.anasexpenses.budget.data.local.entity.BankTemplateEntity
import com.anasexpenses.budget.data.local.entity.RuleEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.RuleSource
import com.anasexpenses.budget.data.local.entity.TxSource
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.domain.PrdConstants
import com.anasexpenses.budget.domain.dedup.DedupHash
import com.anasexpenses.budget.domain.dedup.DedupMatcher
import com.anasexpenses.budget.domain.manual.ManualLineParser
import com.anasexpenses.budget.domain.merchant.MerchantNormalizer
import com.anasexpenses.budget.domain.time.YearMonthRange
import com.anasexpenses.budget.domain.time.epochMillisFrom
import com.anasexpenses.budget.sms.parser.ParseOutcome
import com.anasexpenses.budget.sms.parser.ParsedBankSms
import com.anasexpenses.budget.sms.parser.RegexBankSmsParser
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val ruleDao: RuleDao,
    private val bankTemplateDao: BankTemplateDao,
    private val alertCoordinator: BudgetAlertCoordinator,
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun observeTransactionsForMonth(month: YearMonth): Flow<List<TransactionEntity>> {
        val r = YearMonthRange.epochDayRangeInclusive(month)
        return transactionDao.observeBetweenDays(r.first, r.last)
    }

    fun observeNeedsReviewCount(): Flow<Int> = transactionDao.observeNeedsReviewCount()

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

        val dedupHash = DedupHash.hash(
            cardLast4 = fields.cardLast4,
            amountMilliJod = fields.amountMilliJod,
            instantEpochMillis = instantMillis,
            merchantToken = token,
        )

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
                dedupHash = dedupHash,
                bankTemplateId = bankTemplateId,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        alertCoordinator.refreshAlerts(YearMonth.from(LocalDate.ofEpochDay(fields.dateEpochDay)))
    }

    suspend fun insertManualLine(
        rawLine: String,
        chosenCategoryId: Long?,
    ): Boolean {
        val parsed = ManualLineParser.parse(rawLine) ?: return false
        val (label, milli) = parsed
        val norm = MerchantNormalizer.normalizedMerchant(label)
        val token = MerchantNormalizer.merchantToken(label)
        val categoryId = chosenCategoryId ?: ruleDao.getByMerchantToken(token)?.categoryId
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        val dedupHash = DedupHash.hash(null, milli, epochMillisFrom(today.toEpochDay(), 0, zone), token)
        transactionDao.insert(
            TransactionEntity(
                amountMilliJod = milli,
                currency = "JOD",
                merchant = label,
                normalizedMerchant = norm,
                normalizedMerchantToken = token,
                categoryId = categoryId,
                dateEpochDay = today.toEpochDay(),
                timeSecondOfDay = java.time.LocalTime.now(zone).toSecondOfDay(),
                source = TxSource.MANUAL,
                confidence = 1f,
                status = TxStatus.MANUAL,
                isRefund = false,
                rawSms = null,
                cardLast4 = null,
                dedupHash = dedupHash,
                bankTemplateId = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        alertCoordinator.refreshAlerts(YearMonth.from(today))
        return true
    }

    suspend fun updateTransaction(entity: TransactionEntity) {
        transactionDao.update(entity.copy(updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun assignCategoryAndOptionalRule(
        transactionId: Long,
        categoryId: Long,
        createRule: Boolean,
        backApplyUncategorizedSameMonth: Boolean,
    ) {
        val t = transactionDao.getById(transactionId) ?: return
        val token = t.normalizedMerchantToken
        val now = System.currentTimeMillis()
        transactionDao.update(
            t.copy(
                categoryId = categoryId,
                updatedAtEpochMillis = now,
            ),
        )
        if (createRule) {
            ruleDao.insertReplace(
                RuleEntity(
                    merchantToken = token,
                    categoryId = categoryId,
                    source = RuleSource.USER_CORRECTION,
                    createdAtEpochMillis = now,
                ),
            )
        }
        if (backApplyUncategorizedSameMonth) {
            val ym = YearMonth.from(LocalDate.ofEpochDay(t.dateEpochDay))
            val range = YearMonthRange.epochDayRangeInclusive(ym)
            transactionDao.backApplyCategoryForToken(
                token = token,
                catId = categoryId,
                startDay = range.first,
                endDay = range.last,
                now = now,
            )
        }
        alertCoordinator.refreshAlerts(YearMonth.from(LocalDate.ofEpochDay(t.dateEpochDay)))
    }

    suspend fun getTransaction(id: Long): TransactionEntity? = transactionDao.getById(id)
}
