package com.anasexpenses.budget.data

import com.anasexpenses.budget.alerts.BudgetAlertCoordinator
import com.anasexpenses.budget.data.metrics.AppMetricsRepository
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
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.time.BudgetCycle
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val ruleDao: RuleDao,
    private val bankTemplateDao: BankTemplateDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val alertCoordinator: BudgetAlertCoordinator,
    private val appMetricsRepository: AppMetricsRepository,
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun observeTransactionsForMonth(month: YearMonth): Flow<List<TransactionEntity>> =
        userPreferencesRepository.budgetCycleStartDay.flatMapLatest { startDay ->
            val r = BudgetCycle.epochDayRangeInclusive(month, startDay)
            transactionDao.observeBetweenDays(r.first, r.last)
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
        val rule = ruleDao.getByMerchantToken(token)
        val categoryId: Long? = rule?.categoryId
        val status =
            when {
                categoryId != null -> TxStatus.AUTO
                confidence >= PrdConstants.CONFIDENCE_AUTO_MIN -> TxStatus.AUTO
                else -> TxStatus.NEEDS_REVIEW
            }
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
        appMetricsRepository.recordSmsTransactionInserted()
        val cycleStartDay = userPreferencesRepository.budgetCycleStartDay.first()
        val txnDate = LocalDate.ofEpochDay(fields.dateEpochDay)
        alertCoordinator.refreshAlerts(BudgetCycle.labeledYearMonthForDate(txnDate, cycleStartDay))
    }

    suspend fun insertManualLine(
        rawLine: String,
        chosenCategoryId: Long?,
    ): Boolean {
        val parsed = ManualLineParser.parse(rawLine) ?: return false
        val (label, milli) = parsed
        val norm = MerchantNormalizer.normalizedMerchant(label)
        val token = MerchantNormalizer.merchantToken(label)
        val categoryId = chosenCategoryId?.takeIf { it > 0L }
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
        appMetricsRepository.recordManualTransactionInserted()
        val cycleStartDay = userPreferencesRepository.budgetCycleStartDay.first()
        alertCoordinator.refreshAlerts(BudgetCycle.labeledYearMonthForDate(today, cycleStartDay))
        return true
    }

    suspend fun updateTransaction(entity: TransactionEntity) {
        transactionDao.update(entity.copy(updatedAtEpochMillis = System.currentTimeMillis()))
        val cycleStartDay = userPreferencesRepository.budgetCycleStartDay.first()
        val ym = BudgetCycle.labeledYearMonthForDate(LocalDate.ofEpochDay(entity.dateEpochDay), cycleStartDay)
        alertCoordinator.refreshAlerts(ym)
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
            val cycleStartDay = userPreferencesRepository.budgetCycleStartDay.first()
            val ym = BudgetCycle.labeledYearMonthForDate(LocalDate.ofEpochDay(t.dateEpochDay), cycleStartDay)
            val range = BudgetCycle.epochDayRangeInclusive(ym, cycleStartDay)
            transactionDao.backApplyCategoryForToken(
                token = token,
                catId = categoryId,
                startDay = range.first,
                endDay = range.last,
                now = now,
            )
        }
        val cycleStartDayRefresh = userPreferencesRepository.budgetCycleStartDay.first()
        alertCoordinator.refreshAlerts(
            BudgetCycle.labeledYearMonthForDate(LocalDate.ofEpochDay(t.dateEpochDay), cycleStartDayRefresh),
        )
    }

    suspend fun getTransaction(id: Long): TransactionEntity? = transactionDao.getById(id)
}
