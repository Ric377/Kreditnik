@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kreditnik.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState

/**
 * Вспомогательный Composable для создания вертикального скроллируемого селектора чисел.
 * @param items Список строк для отображения.
 * @param state Состояние, хранящее текущий выбранный индекс (в оригинальном диапазоне).
 * @param modifier Модификатор для настройки внешнего вида.
 * @param itemHeight Высота каждого элемента в списке.
 * @param endlessScroll Должен ли список прокручиваться бесконечно (зацикленно).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberPicker(
    items: List<String>,
    state: MutableState<Int>,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    endlessScroll: Boolean = false,
    autoSnap: Boolean = true
) {
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    /* ---------- расширенный список ---------- */

    val cycles = 5                                      // 5 полных копий => «бесконечность»
    val extended = remember(items, endlessScroll) {
        if (endlessScroll) buildList {
            repeat(cycles) { addAll(items) }            // …23 0 1 2 … 23 0 1 …
        } else items
    }
    val originStart = if (endlessScroll) items.size * (cycles / 2) else 0

    /* ---------- состояние прокрутки ---------- */

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (endlessScroll)
            originStart + state.value - 1      // ← вернуть “−1”
        else
            (state.value - 1).coerceAtLeast(0)
    )

    /* ---------- «живой» центральный индекс ---------- */

    val centerOrig by remember(listState) {
        derivedStateOf {
            val centerExt =
                listState.firstVisibleItemIndex + 1 +
                        (listState.firstVisibleItemScrollOffset / itemHeightPx).roundToInt()
            if (endlessScroll) {
                val raw = centerExt - originStart
                ((raw % items.size) + items.size) % items.size       // floorMod
            } else {
                centerExt.coerceIn(0, items.size - 1)
            }
        }
    }

    /* ---------- синхронизация ---------- */

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            if (state.value != centerOrig) state.value = centerOrig
            if (endlessScroll && autoSnap) {
                val target = originStart + centerOrig - 1
                if (listState.firstVisibleItemIndex != target) {
                    listState.scrollToItem(target)                  // мгновенно, без задержки
                }
            }
        }
    }

    LaunchedEffect(state.value) {
        if (endlessScroll && autoSnap && !listState.isScrollInProgress) {
            listState.scrollToItem(originStart + state.value - 1)
        }

    }

    /* ---------- UI ---------- */

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(extended.size) { idxExt ->
                val idxOrig = if (endlessScroll) {
                    val raw = idxExt - originStart
                    ((raw % items.size) + items.size) % items.size
                } else idxExt
                val selected = idxOrig == centerOrig
                Text(
                    text = extended[idxExt],
                    style = if (selected)
                        MaterialTheme.typography.headlineMedium
                    else
                        MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .height(itemHeight)
                        .alpha(if (selected) 1f else 0.5f)
                )
            }
        }
        Divider(
            Modifier.align(Alignment.Center)
                .offset(y = -itemHeight / 2)
                .padding(horizontal = 20.dp)
        )
        Divider(
            Modifier.align(Alignment.Center)
                .offset(y = itemHeight / 2)
                .padding(horizontal = 20.dp)
        )
    }
}






@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val darkModeEnabled by settingsViewModel.darkModeEnabled.collectAsState()
    val defaultCurrency by settingsViewModel.defaultCurrency.collectAsState()
    val reminderDays by settingsViewModel.reminderDaysBefore.collectAsState()
    val reminderTime by settingsViewModel.reminderTime.collectAsState()

    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var textButtonWidthPx by remember { mutableStateOf(0) }
    var textButtonHeightPx by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки", style = MaterialTheme.typography.headlineSmall) }
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
            // Раздел "Общие"
            Text(
                text = "Общие",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
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
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        TextButton(
                            onClick = { currencyMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    textButtonWidthPx = coordinates.size.width
                                    textButtonHeightPx = coordinates.size.height
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
                            offset = DpOffset(x = 0.dp, y = with(density) { textButtonHeightPx.toDp() } - 103.dp),
                            modifier = Modifier
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
                                    text = { Text(text = currency, style = MaterialTheme.typography.bodyLarge) }
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
                        onCheckedChange = { settingsViewModel.setDarkMode(it) }
                    )
                }
            }

            // ---
            // Раздел "Уведомления"
            Text(
                text = "Уведомления",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDayPicker = true }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "За сколько дней напомнить",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$reminderDays дн.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Выбрать количество дней")
                    }

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "Время напоминания",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = reminderTime,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Выбрать время")
                    }
                }
            }

            // ---
            // Раздел "О приложении"
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("О приложении", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyPolicyDialog = true }
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Политика конфиденциальности", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }

    // Диалог выбора количества дней
    if (showDayPicker) {
        val daysList = (0..7).map { it.toString() }
        val selectedDayIndex = remember(reminderDays) {
            mutableStateOf(reminderDays.coerceIn(0, 7))
        }

        AlertDialog(
            onDismissRequest = { showDayPicker = false },
            title = { Text("Выберите количество дней") },
            text = {
                NumberPicker(
                    items = daysList,
                    state = selectedDayIndex,
                    modifier = Modifier.height(120.dp),
                    endlessScroll = true,
                    autoSnap = false
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Log.d("ReminderTest", "⚙️ Установлено: напоминать за ${selectedDayIndex.value} дней")

                    settingsViewModel.setReminderDaysBefore(selectedDayIndex.value)
                    showDayPicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDayPicker = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог выбора времени
    if (showTimePicker) {
        val parsedTime = remember(reminderTime) {
            try { LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm")) }
            catch (e: Exception) { LocalTime.of(12, 0) }
        }
        val hoursList = (0..23).map { "%02d".format(it) }
        val minutesList = (0..59).map { "%02d".format(it) }
        val selectedHourIndex = remember { mutableStateOf(parsedTime.hour) }
        val selectedMinuteIndex = remember { mutableStateOf(parsedTime.minute) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Выберите время") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NumberPicker(
                        items = hoursList,
                        state = selectedHourIndex,
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        endlessScroll = true // Включаем бесконечную прокрутку для часов
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                    NumberPicker(
                        items = minutesList,
                        state = selectedMinuteIndex,
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        endlessScroll = true // Включаем бесконечную прокрутку для минут
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val formatted = String.format("%02d:%02d", selectedHourIndex.value, selectedMinuteIndex.value)
                    Log.d("ReminderTest", "⏰ Установлено время напоминания: $formatted")
                    settingsViewModel.setReminderTime(formatted)
                    showTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            }
        )
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
                TextButton(onClick = { showAboutDialog = false }) { Text("ОК") }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Политика конфиденциальности") },
            text = {
                val policyText by remember {
                    mutableStateOf(
                        context.assets.open("privacy_policy_ru.md").bufferedReader().use { it.readText() }
                    )
                }
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(policyText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) { Text("ОК") }
            }
        )
    }
}