package com.anasexpenses.budget.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
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
