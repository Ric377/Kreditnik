// SettingsViewModel.kt
package com.kreditnik.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

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
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsDataStore.setReminderTime(time)
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
}

