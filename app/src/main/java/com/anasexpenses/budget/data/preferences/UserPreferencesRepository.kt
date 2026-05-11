package com.anasexpenses.budget.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.anasexpenses.budget.domain.time.BudgetCycle
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.userPreferencesDataStore

    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val smsPermissionSkipped = booleanPreferencesKey("sms_permission_skipped")
        val selectedYearMonth = stringPreferencesKey("selected_year_month")
        val budgetCycleStartDay = intPreferencesKey("budget_cycle_start_day")
        val dailyBackupTreeUri = stringPreferencesKey("daily_backup_tree_uri")
    }

    /**
     * Which calendar day each budget month begins (1–28). Spending for labeled month YYYY-MM runs from that
     * day through the day before the same calendar day next month.
     */
    val budgetCycleStartDay: Flow<Int> = store.data
        .map { prefs ->
            BudgetCycle.clampStartDay(prefs[Keys.budgetCycleStartDay] ?: BudgetCycle.MIN_START_DAY)
        }
        .distinctUntilChanged()

    suspend fun setBudgetCycleStartDay(day: Int) {
        val clamped = BudgetCycle.clampStartDay(day)
        store.edit { prefs ->
            prefs[Keys.budgetCycleStartDay] = clamped
            val ym = BudgetCycle.labeledYearMonthForDate(LocalDate.now(), clamped)
            prefs[Keys.selectedYearMonth] = ym.toString()
        }
    }

    /** Budget UI month (Home + Transactions). Defaults to labeled month for today using [budgetCycleStartDay]. */
    val selectedMonth: Flow<YearMonth> = store.data
        .map { prefs ->
            val parsed = prefs[Keys.selectedYearMonth]?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            val cycle = BudgetCycle.clampStartDay(prefs[Keys.budgetCycleStartDay] ?: BudgetCycle.MIN_START_DAY)
            parsed ?: BudgetCycle.labeledYearMonthForDate(LocalDate.now(), cycle)
        }
        .distinctUntilChanged()

    suspend fun setSelectedMonth(month: YearMonth) {
        store.edit { it[Keys.selectedYearMonth] = month.toString() }
    }

    val onboardingComplete: Flow<Boolean> = store.data.map { it[Keys.onboardingComplete] ?: false }

    val smsPermissionSkipped: Flow<Boolean> = store.data.map { it[Keys.smsPermissionSkipped] ?: false }

    val dailyBackupTreeUri: Flow<String?> = store.data.map { it[Keys.dailyBackupTreeUri] }

    suspend fun setOnboardingComplete(value: Boolean) {
        store.edit { it[Keys.onboardingComplete] = value }
    }

    suspend fun setSmsPermissionSkipped(value: Boolean) {
        store.edit { it[Keys.smsPermissionSkipped] = value }
    }

    suspend fun setDailyBackupTreeUri(uri: String?) {
        store.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(Keys.dailyBackupTreeUri)
            } else {
                prefs[Keys.dailyBackupTreeUri] = uri
            }
        }
    }
}
