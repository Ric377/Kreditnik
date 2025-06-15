// SettingsDataStore.kt
package com.kreditnik.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Создаем DataStore
private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val REMINDER_DAYS_BEFORE = intPreferencesKey("reminder_days_before")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
    }

    val darkModeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_ENABLED] ?: false
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }

    val defaultCurrencyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_CURRENCY] ?: "RUB"
        }

    val reminderDaysBeforeFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDER_DAYS_BEFORE] ?: 1
        }

    val reminderTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDER_TIME] ?: "12:00"
        }

    suspend fun setReminderDaysBefore(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_DAYS_BEFORE] = days
        }
    }

    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_TIME] = time
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_CURRENCY] = currency
        }
    }
}
