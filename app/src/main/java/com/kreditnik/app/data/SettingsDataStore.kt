package com.kreditnik.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Создание экземпляра DataStore на уровне всего приложения.
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Класс для управления настройками приложения с использованием Jetpack DataStore.
 * Обеспечивает асинхронный и безопасный доступ к сохраненным данным.
 *
 * @param context Контекст приложения, необходимый для инициализации DataStore.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val REMINDER_DAYS_BEFORE = intPreferencesKey("reminder_days_before")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
    }

    /** Поток, предоставляющий текущее состояние темной темы. */
    val darkModeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_ENABLED] ?: false
        }

    /** Поток, предоставляющий выбранную валюту по умолчанию. */
    val defaultCurrencyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_CURRENCY] ?: "₽" // Значение по умолчанию - Рубль
        }

    /** Поток, предоставляющий количество дней для напоминания о платеже. */
    val reminderDaysBeforeFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDER_DAYS_BEFORE] ?: 1 // По умолчанию - 1 день
        }

    /** Поток, предоставляющий время для напоминания о платеже. */
    val reminderTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDER_TIME] ?: "12:00" // По умолчанию - 12:00
        }

    /**
     * Сохраняет выбранное состояние темной темы.
     * @param enabled `true` для включения темной темы, `false` для выключения.
     */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }

    /**
     * Сохраняет выбранную валюту по умолчанию.
     * @param currency Символ валюты (например, "₽", "$").
     */
    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_CURRENCY] = currency
        }
    }

    /**
     * Сохраняет количество дней для напоминания.
     * @param days Количество дней до платежа.
     */
    suspend fun setReminderDaysBefore(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_DAYS_BEFORE] = days
        }
    }

    /**
     * Сохраняет время для напоминания.
     * @param time Время в формате "ЧЧ:мм".
     */
    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_TIME] = time
        }
    }
}