@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kreditnik.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import androidx.compose.ui.platform.LocalContext



@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val darkModeEnabled by settingsViewModel.darkModeEnabled.collectAsState()
    val defaultCurrency by settingsViewModel.defaultCurrency.collectAsState()

    val context = LocalContext.current

    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }



    val density = LocalDensity.current

    // Состояния для хранения ширины и высоты TextButton
    var textButtonWidthPx by remember { mutableStateOf(0) }
    var textButtonHeightPx by remember { mutableStateOf(0) }
    // Состояние для хранения глобальной позиции TextButton
    var textButtonGlobalPositionX by remember { mutableStateOf(0f) } // <-- ВОЗВРАЩАЕМ X
    var textButtonGlobalPositionY by remember { mutableStateOf(0f) }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Настройки", style = MaterialTheme.typography.headlineSmall)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Валюта по умолчанию
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Валюта по умолчанию",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        // Убираем wrapContentSize(align = Alignment.TopEnd) для Box,
                        // если хотим, чтобы меню выпадало из левого края кнопки.
                        // Просто wrapContentSize() или без него, если TextButton сам определяет размер
                        modifier = Modifier.wrapContentSize(Alignment.TopStart) // Чтобы Box соответствовал TopStart кнопки
                    ) {
                        TextButton(
                            onClick = { currencyMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    // Получаем размеры и глобальную позицию TextButton
                                    textButtonWidthPx = coordinates.size.width
                                    textButtonHeightPx = coordinates.size.height
                                    textButtonGlobalPositionX = coordinates.positionInWindow().x // <-- ВОЗВРАЩАЕМ X
                                    textButtonGlobalPositionY = coordinates.positionInWindow().y
                                }
                        ) {
                            Text(
                                text = defaultCurrency,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false },
                            offset = DpOffset(
                                x = 0.dp,
                                y = with(density) { textButtonHeightPx.toDp() } - 103.dp
                            ),


                            properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
                            modifier = Modifier
                                // Устанавливаем ширину DropdownMenu равной ширине TextButton
                                .width(with(density) { textButtonWidthPx.toDp() })
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            settingsViewModel.availableCurrencies.forEach { currency ->
                                DropdownMenuItem(
                                    onClick = {
                                        settingsViewModel.setDefaultCurrency(currency)
                                        currencyMenuExpanded = false
                                    },
                                    text = {
                                        Text(
                                            text = currency,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Темная тема
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { settingsViewModel.setDarkMode(!darkModeEnabled) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Темная тема",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = {
                            settingsViewModel.setDarkMode(it)
                        }
                    )
                }
            }

            // Объединённый блок
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(140.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { showAboutDialog = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("О приложении", style = MaterialTheme.typography.titleMedium)
                    }
                    Divider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { showPrivacyPolicyDialog = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Политика конфиденциальности", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("О приложении Кредитник") },
            text = {
                Column {
                    Text("Версия: 1.0.0")
                    Text("Разработано: Челидзе Ричард")
                    Spacer(Modifier.height(8.dp))
                    Text("Кредитник — ваш помощник в управлении кредитами.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("ОК")
                }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Политика конфиденциальности") },
            text = {
                // читаем файл assets/privacy_policy_ru.md один раз
                val policyText by remember {
                    mutableStateOf(
                        context.assets.open("privacy_policy_ru.md")
                            .bufferedReader()
                            .use { it.readText() }
                    )
                }
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(policyText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("ОК")
                }
            }
        )
    }
}