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

    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- Переключатель темы ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Темная тема",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = { settingsViewModel.setDarkMode(it) }
            )
        }

        // --- Выбор валюты ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Валюта по умолчанию",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Box {
                // Делаем TextButton без рамки
                TextButton(
                    onClick = { currencyMenuExpanded = true },
                    modifier = Modifier.height(36.dp) // 36dp как Switch
                ) {
                    Text(
                        text = defaultCurrency,
                        style = MaterialTheme.typography.bodyLarge // размер текста как у переключателя
                    )
                }

                DropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false }
                ) {
                    settingsViewModel.availableCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                settingsViewModel.setDefaultCurrency(currency)
                                currencyMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
