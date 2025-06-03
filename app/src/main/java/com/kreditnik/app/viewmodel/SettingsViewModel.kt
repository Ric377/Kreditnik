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
        .stateIn(viewModelScope, SharingStarted.Lazily, "RUB")

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
