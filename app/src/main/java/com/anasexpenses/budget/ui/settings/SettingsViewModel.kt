package com.anasexpenses.budget.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.category.CategoryBulkImport
import com.anasexpenses.budget.domain.time.BudgetCycle
import com.anasexpenses.budget.data.metrics.AppMetricsRepository
import com.anasexpenses.budget.data.export.DatabaseExportHelper
import com.anasexpenses.budget.sms.SmsInboxBackfill
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryImportSummary(
    val addedCount: Int,
    val skippedDuplicateCount: Int,
    val parseErrors: List<String>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseExportHelper: DatabaseExportHelper,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val smsInboxBackfill: SmsInboxBackfill,
    private val userPreferencesRepository: UserPreferencesRepository,
    appMetricsRepository: AppMetricsRepository,
) : ViewModel() {

    val budgetCycleStartDay: StateFlow<Int> =
        userPreferencesRepository.budgetCycleStartDay.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BudgetCycle.MIN_START_DAY,
        )

    fun setBudgetCycleStartDay(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetCycleStartDay(day)
        }
    }

    val smsTransactionRows: StateFlow<Long> =
        appMetricsRepository.smsTransactionRows.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0L,
        )
    val manualTransactionRows: StateFlow<Long> =
        appMetricsRepository.manualTransactionRows.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0L,
        )

    fun exportToUri(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    databaseExportHelper.checkpointAndCopyTo(out)
                } ?: error("no stream")
            }.isSuccess
            onDone(ok)
        }
    }

    fun runInboxBackfill(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val bodies = smsInboxBackfill.collectRecentBankBodies()
            transactionRepository.ingestSmsBodies(bodies)
            onDone(bodies.size)
        }
    }

    fun ingestPastedSms(body: String, onDone: (Boolean) -> Unit) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            transactionRepository.ingestSmsBodies(listOf(trimmed))
            onDone(true)
        }
    }

    fun openPrivacyPolicyUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Adds categories for the **currently selected** budget month; skips names that already exist (case-insensitive). */
    fun importCategoriesFromText(text: String, onDone: (CategoryImportSummary) -> Unit) {
        viewModelScope.launch {
            val parsed = CategoryBulkImport.parseLines(text)
            val month = userPreferencesRepository.selectedMonth.first()
            val monthKey = month.toString()
            val existing =
                categoryRepository.getByMonth(monthKey).map { it.name.trim().lowercase() }.toMutableSet()
            var added = 0
            var skippedDup = 0
            for (line in parsed.lines) {
                val key = line.name.lowercase()
                if (key in existing) {
                    skippedDup++
                    continue
                }
                categoryRepository.addCategory(monthKey, line.name, line.milliJod, excludedFromSpend = false)
                existing.add(key)
                added++
            }
            onDone(
                CategoryImportSummary(
                    addedCount = added,
                    skippedDuplicateCount = skippedDup,
                    parseErrors = parsed.lineErrors,
                ),
            )
        }
    }
}
