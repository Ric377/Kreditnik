package com.kreditnik.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.SettingsDataStore
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.data.DatabaseProvider
import com.kreditnik.app.util.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first



class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(context)
    private val loanRepository = LoanRepository(
        DatabaseProvider.getDatabase(context).loanDao()
    )

    val darkModeEnabled = settingsDataStore.darkModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val defaultCurrency = settingsDataStore.defaultCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "₽")

    val availableCurrencies = listOf("$", "€", "£", "₽", "¥", "₾")

    val reminderDaysBefore = settingsDataStore.reminderDaysBeforeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    val reminderTime = settingsDataStore.reminderTimeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "12:00")

    fun setReminderDaysBefore(days: Int) {
        viewModelScope.launch {
            settingsDataStore.setReminderDaysBefore(days)
            rescheduleAllReminders()
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsDataStore.setReminderTime(time)
            rescheduleAllReminders()
        }
    }



    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDarkMode(enabled)
        }
    }

    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch {
            settingsDataStore.setDefaultCurrency(currency)
        }
    }

    private fun rescheduleAllReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            val newTime = settingsDataStore.reminderTimeFlow.first()
            val newDays = settingsDataStore.reminderDaysBeforeFlow.first()

            val loans = loanRepository.getAllLoans()
            loans.filter { it.reminderEnabled }.forEach { loan ->
                val updated = loan.copy(
                    reminderTime = newTime,
                    reminderDaysBefore = newDays
                )
                loanRepository.updateLoan(updated)
                NotificationHelper.scheduleLoanReminder(context, updated)
            }
        }
    }

}
