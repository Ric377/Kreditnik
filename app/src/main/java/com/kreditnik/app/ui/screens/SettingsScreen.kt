// SettingsScreen.kt
package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import androidx.compose.ui.Alignment


@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val darkModeEnabled by settingsViewModel.darkModeEnabled.collectAsState()
    val defaultCurrency by settingsViewModel.defaultCurrency.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Темная тема", Modifier.weight(1f))
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = { settingsViewModel.setDarkMode(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Валюта по умолчанию: $defaultCurrency")

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            settingsViewModel.setDefaultCurrency("USD")
        }) {
            Text("Сменить валюту на USD")
        }
    }
}
