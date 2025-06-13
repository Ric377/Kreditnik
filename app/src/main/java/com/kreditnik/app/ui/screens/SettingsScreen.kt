package com.kreditnik.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp // Импорт для sp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight // Для стрелки "далее"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val darkModeEnabled by settingsViewModel.darkModeEnabled.collectAsState()
    val defaultCurrency by settingsViewModel.defaultCurrency.collectAsState()

    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) } // Состояние для показа диалога Политики конфиденциальности

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // Добавляем прокрутку
        ) {
            // Раздел: Общие настройки
            SettingSectionHeader(title = "Общие")
            Divider()

            // Настройка: Валюта по умолчанию
            ListItem(
                headlineContent = { Text("Валюта по умолчанию") },
                supportingContent = { Text("Выбранная валюта: $defaultCurrency") },
                trailingContent = {
                    Box {
                        TextButton(
                            onClick = { currencyMenuExpanded = true },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = defaultCurrency,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp) // <-- Изменено на 20sp, чтобы лучше вписывалось
                            )

                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Открыть выбор валюты")
                        }

                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            settingsViewModel.availableCurrencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = currency,
                                            style = MaterialTheme.typography.bodyLarge // <-- Применяем bodyLarge для пунктов списка
                                        )
                                    },
                                    onClick = {
                                        settingsViewModel.setDefaultCurrency(currency)
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currencyMenuExpanded = true } // Делаем всю строку кликабельной
            )
            Divider()

            // Раздел: Визуальные настройки
            SettingSectionHeader(title = "Визуальные")
            Divider()

            // Настройка: Темная тема
            ListItem(
                headlineContent = { Text("Темная тема") },
                supportingContent = { Text("Включить или выключить темную тему") },
                trailingContent = {
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { enabled ->
                            settingsViewModel.setDarkMode(enabled)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsViewModel.setDarkMode(!darkModeEnabled) } // Переключаем по клику на всю строку
            )
            Divider()

            // Дополнительные настройки (если появятся)
            SettingSectionHeader(title = "Дополнительно")
            Divider()

            ListItem(
                headlineContent = { Text("О приложении") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutDialog = true } // Теперь клик открывает диалог
            )
            Divider()

            // НОВЫЙ ПУНКТ: Политика конфиденциальности
            ListItem(
                headlineContent = { Text("Политика конфиденциальности") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyPolicyDialog = true } // Открывает диалог с политикой
            )
            Divider()
        }
    }

    // Диалог "О приложении"
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false }, // Закрыть диалог при нажатии вне его
            title = { Text("О приложении Кредитник") },
            text = {
                Column {
                    Text("Версия: 1.0.0") // Можете заменить на BuildConfig.VERSION_NAME если у вас есть
                    Text("Разработано: Челидзе Ричард")
                    Spacer(Modifier.height(8.dp))
                    Text("Кредитник - ваш удобный помощник в управлении кредитами.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("ОК")
                }
            }
        )
    }

    // Диалог "Политика конфиденциальности"
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false }, // Закрыть диалог при нажатии вне его
            title = { Text("Политика конфиденциальности") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) { // Делаем текст прокручиваемым
                    Text("Дата вступления в силу: 13 Июня 2025") // Автоматически установленная дата

                    Spacer(Modifier.height(8.dp))
                    Text("1. Сбор и хранение данных")
                    Text("Приложение Kreditnik (далее \"Приложение\") предназначено для персонального управления вашими финансовыми обязательствами (кредитами и долгами). Все данные, которые вы вводите в Приложение (такие как названия кредитов, суммы, процентные ставки, даты платежей, история платежей), хранятся ИСКЛЮЧИТЕЛЬНО на вашем мобильном устройстве.")
                    Text("Приложение НЕ собирает, НЕ передает и НЕ хранит ваши персональные данные на внешних серверах или в облачных хранилищах.")
                    Text("Приложение НЕ использует сторонние сервисы для аналитики, рекламы или сбора информации о пользователях.")

                    Spacer(Modifier.height(8.dp))
                    Text("2. Использование данных")
                    Text("Ваши данные используются исключительно для предоставления функционала Приложения: расчета платежей, отслеживания задолженностей, начисления процентов и отображения информации о ваших финансовых обязательствах. Мы не используем ваши данные для каких-либо других целей.")

                    Spacer(Modifier.height(8.dp))
                    Text("3. Безопасность данных")
                    Text("Поскольку все данные хранятся локально на вашем устройстве, безопасность данных в первую очередь зависит от безопасности вашего устройства. Мы стремимся обеспечить защиту данных в рамках функционала Приложения, но не можем гарантировать безопасность устройства пользователя.")

                    Spacer(Modifier.height(8.dp))
                    Text("4. Передача данных третьим лицам")
                    Text("Ваши личные данные не передаются, не продаются и не обмениваются с третьими лицами, за исключением случаев, когда это явно разрешено вами (например, функция экспорта данных, если она будет реализована).")

                    Spacer(Modifier.height(8.dp))
                    Text("5. Права пользователя")
                    Text("Вы имеете полный контроль над своими данными в Приложении. Вы можете просматривать, изменять и удалять любые введенные вами данные непосредственно через интерфейс Приложения.")

                    Spacer(Modifier.height(8.dp))
                    Text("6. Изменения в политике конфиденциальности")
                    Text("Мы можем периодически обновлять нашу Политику конфиденциальности. Мы уведомим вас о любых изменениях, опубликовав новую Политику конфиденциальности в Приложении. Рекомендуется периодически просматривать эту Политику конфиденциальности на предмет изменений.")

                    Spacer(Modifier.height(8.dp))
                    Text("7. Контактная информация")
                    Text("Если у вас есть вопросы или предложения относительно нашей Политики конфиденциальности, пожалуйста, свяжитесь с нами по адресу: ric.ch@yandex.ru") // Замените на свой реальный email
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

// Вспомогательный Composable для заголовков секций
@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary, // Или secondary, в зависимости от палитры
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}