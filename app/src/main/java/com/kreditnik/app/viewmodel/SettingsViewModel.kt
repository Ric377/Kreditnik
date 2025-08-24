package com.kreditnik.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.DatabaseProvider
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.data.SettingsDataStore
import com.kreditnik.app.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel для управления настройками приложения.
 * Взаимодействует с [SettingsDataStore] для сохранения и получения
 * пользовательских настроек.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(context)
    private val loanRepository = LoanRepository(
        DatabaseProvider.getDatabase(context).loanDao()
    )

    /** [StateFlow] для отслеживания состояния темной темы. */
    val darkModeEnabled = settingsDataStore.darkModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** [StateFlow] для отслеживания валюты по умолчанию. */
    val defaultCurrency = settingsDataStore.defaultCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "₽")

    /** Список доступных для выбора валют. */
    val availableCurrencies = listOf("$", "€", "£", "₽", "¥", "₾")

    /** [StateFlow] для отслеживания количества дней для отправки напоминания. */
    val reminderDaysBefore = settingsDataStore.reminderDaysBeforeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    /** [StateFlow] для отслеживания времени отправки напоминания. */
    val reminderTime = settingsDataStore.reminderTimeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "12:00")

    /**
     * Сохраняет состояние темной темы.
     * @param enabled `true` для включения темной темы.
     */
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDarkMode(enabled)
        }
    }

    /**
     * Сохраняет выбранную валюту по умолчанию.
     * @param currency Символ валюты.
     */
    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch {
            settingsDataStore.setDefaultCurrency(currency)
        }
    }

    /**
     * Сохраняет количество дней для напоминания и перепланирует все уведомления.
     * @param days Количество дней.
     */
    fun setReminderDaysBefore(days: Int) {
        viewModelScope.launch {
            settingsDataStore.setReminderDaysBefore(days)
            rescheduleAllReminders()
        }
    }

    /**
     * Сохраняет время для напоминания и перепланирует все уведомления.
     * @param time Время в формате "ЧЧ:мм".
     */
    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsDataStore.setReminderTime(time)
            rescheduleAllReminders()
        }
    }

    /**
     * Перепланирует все активные напоминания на основе новых глобальных настроек времени.
     * Вызывается при изменении [reminderDaysBefore] или [reminderTime].
     */
    private fun rescheduleAllReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            val newTime = settingsDataStore.reminderTimeFlow.first()
            val newDays = settingsDataStore.reminderDaysBeforeFlow.first()

            val loans = loanRepository.getAllLoans()
            loans.filter { it.reminderEnabled }.forEach { loan ->
                val updatedLoan = loan.copy(
                    reminderTime = newTime,
                    reminderDaysBefore = newDays
                )
                loanRepository.updateLoan(updatedLoan)
                NotificationHelper.scheduleLoanReminder(context, updatedLoan)
            }
        }
    }
}