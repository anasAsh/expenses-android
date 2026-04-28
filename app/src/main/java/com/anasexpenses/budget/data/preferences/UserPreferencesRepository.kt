package com.anasexpenses.budget.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
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
    }

    /** Budget UI month (Home + Transactions). Defaults to current calendar month. */
    val selectedMonth: Flow<YearMonth> = store.data
        .map { prefs ->
            prefs[Keys.selectedYearMonth]?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                ?: YearMonth.now()
        }
        .distinctUntilChanged()

    suspend fun setSelectedMonth(month: YearMonth) {
        store.edit { it[Keys.selectedYearMonth] = month.toString() }
    }

    val onboardingComplete: Flow<Boolean> = store.data.map { it[Keys.onboardingComplete] ?: false }

    val smsPermissionSkipped: Flow<Boolean> = store.data.map { it[Keys.smsPermissionSkipped] ?: false }

    suspend fun setOnboardingComplete(value: Boolean) {
        store.edit { it[Keys.onboardingComplete] = value }
    }

    suspend fun setSmsPermissionSkipped(value: Boolean) {
        store.edit { it[Keys.smsPermissionSkipped] = value }
    }
}
