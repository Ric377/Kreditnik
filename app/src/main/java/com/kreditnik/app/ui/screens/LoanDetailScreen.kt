// LoanDetailScreen.kt
package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import com.kreditnik.app.util.NotificationHelper
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions
import java.util.*
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import android.widget.Toast
import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loan: Loan,
    settingsViewModel: SettingsViewModel,
    navController: NavController,
    loanViewModel: LoanViewModel
) {
    val currency by settingsViewModel.defaultCurrency.collectAsState()
    val expandedMenu = remember { mutableStateOf(false) }
    val showAddDialog = remember { mutableStateOf(false) }
    val showPayDialog = remember { mutableStateOf(false) }
    val amountInput = remember { mutableStateOf("") }

    // Расчет даты следующего платежа
    val nextPaymentDate by remember(loan.startDate, loan.monthlyPaymentDay) {
        derivedStateOf {
            val today = LocalDate.now()
            var nextDate = loan.startDate

            // Если дата открытия уже прошла в текущем месяце, то ищем в следующем
            if (loan.startDate.dayOfMonth > today.dayOfMonth && loan.startDate.month == today.month && loan.startDate.year == today.year) {
                // Если сегодня 15, а дата открытия 20, и месяц текущий, то следующая дата в этом месяце
                nextDate = loan.startDate
            } else if (loan.monthlyPaymentDay == 0) { // "Последний день месяца"
                // Если день платежа "Последний день месяца"
                if (today.dayOfMonth >= today.lengthOfMonth()) {
                    // Если сегодня последний день месяца или позже, то следующий платеж в следующем месяце
                    nextDate = today.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
                } else {
                    nextDate = today.with(TemporalAdjusters.lastDayOfMonth())
                }
            } else {
                // Если день платежа конкретное число
                val targetDay = loan.monthlyPaymentDay
                if (today.dayOfMonth >= targetDay) {
                    // Если текущий день >= дню платежа, то следующий платеж в следующем месяце
                    nextDate = today.plusMonths(1).withDayOfMonth(targetDay)
                } else {
                    // Иначе, следующий платеж в текущем месяце
                    nextDate = today.withDayOfMonth(targetDay)
                }
            }

            // Убедимся, что дата не раньше даты открытия
            if (nextDate.isBefore(loan.startDate)) {
                nextDate = loan.startDate
            }
            nextDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = loan.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { expandedMenu.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = expandedMenu.value,
                        onDismissRequest = { expandedMenu.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                expandedMenu.value = false
                                navController.navigate("editLoan/${loan.id}")
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                expandedMenu.value = false
                                loanViewModel.deleteLoan(loan)
                                navController.popBackStack()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 0.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        )
        {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val totalAmountDue = loan.principal + loan.accruedInterest // Вычисляем общую сумму к погашению

                    LoanDetailItem("Тип кредита", loan.type.displayName)
                    LoanDetailItem("Общая сумма к погашению", "${totalAmountDue.formatMoney()} $currency")
                    LoanDetailItem("Остаток основного долга", "${loan.principal.formatMoney()} $currency")
                    LoanDetailItem("Начисленные проценты", "${loan.accruedInterest.formatMoney()} $currency")
                    LoanDetailItem("Изначальная сумма кредита", "${loan.initialPrincipal.formatMoney()} $currency")
                    LoanDetailItem("Процентная ставка", "${loan.interestRate}%")
                    LoanDetailItem("Срок", "${loan.months} месяцев")
                    LoanDetailItem(
                        "Дата открытия",
                        loan.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    )
                    LoanDetailItem(
                        "Ежемесячный платёж",
                        "${calculateMonthlyPayment(loan.principal, loan.interestRate, loan.months).formatMoney()} $currency"
                    )
                    LoanDetailItem("Дата платежа", nextPaymentDate)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog.value = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить", maxLines = 1)
                }
                Button(
                    onClick = { showPayDialog.value = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Погасить", maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val context = LocalContext.current

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Напоминание о платеже", modifier = Modifier.weight(1f))

                    val context = LocalContext.current
                    val activity = context as? Activity
                    var requested by remember { mutableStateOf(false) }
                    var enableSwitch by remember { mutableStateOf(loan.reminderEnabled) }

                    LaunchedEffect(requested) {
                        if (enableSwitch && requested) {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            val postNotiGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                alarmManager.canScheduleExactAlarms()
                            } else true

                            if (!postNotiGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ActivityCompat.requestPermissions(
                                    activity ?: return@LaunchedEffect,
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    1001
                                )
                            } else if (!alarmGranted) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }

                            // Заново проверим, получены ли оба разрешения
                            val grantedNow = (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED) &&
                                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        alarmManager.canScheduleExactAlarms()
                                    } else true)

                            if (grantedNow) {
                                enableSwitch = true
                                loanViewModel.updateLoan(loan.copy(reminderEnabled = true))
                                NotificationHelper.scheduleLoanReminder(context, loan)
                                Toast.makeText(
                                    context,
                                    "Уведомление установлено на 12:00 за день до платежа",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }


                    val scope = rememberCoroutineScope()

                    Switch(
                        checked = enableSwitch,
                        onCheckedChange = {
                            enableSwitch = it
                            if (it) {
                                scope.launch {
                                    val postNotiGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                    } else true

                                    if (!postNotiGranted) {
                                        ActivityCompat.requestPermissions(
                                            activity ?: return@launch,
                                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                            1001
                                        )
                                        delay(500) // ждём, чтобы система успела обработать
                                    }

                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        alarmManager.canScheduleExactAlarms()
                                    } else true

                                    if (!alarmGranted) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                        delay(500) // снова небольшая пауза
                                    }

                                    val postNotiGrantedNow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                    } else true

                                    val alarmGrantedNow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        alarmManager.canScheduleExactAlarms()
                                    } else true

                                    if (postNotiGrantedNow && alarmGrantedNow) {
                                        loanViewModel.updateLoan(loan.copy(reminderEnabled = true))
                                        NotificationHelper.scheduleLoanReminder(context, loan)
                                        Toast.makeText(
                                            context,
                                            "Уведомление установлено на 12:00 за день до платежа",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        enableSwitch = false
                                        Toast.makeText(
                                            context,
                                            "Разрешения не даны. Напоминание не включено.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } else {
                                loanViewModel.updateLoan(loan.copy(reminderEnabled = false))
                                NotificationHelper.cancelLoanReminder(context, loan)
                                Toast.makeText(context, "Уведомление отменено", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )









                }
            }

        }
    }

    if (showAddDialog.value) {
        AlertDialog(
            onDismissRequest = { showAddDialog.value = false },
            title = { Text("Добавить долг") },
            text = {
                OutlinedTextField(
                    value = amountInput.value,
                    onValueChange = { amountInput.value = it },
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountInput.value.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        loanViewModel.updateLoanPrincipal(loan.id, amount)
                    }
                    amountInput.value = ""
                    showAddDialog.value = false
                }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    amountInput.value = ""
                    showAddDialog.value = false
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showPayDialog.value) {
        AlertDialog(
            onDismissRequest = { showPayDialog.value = false },
            title = { Text("Погасить долг") },
            text = {
                OutlinedTextField(
                    value = amountInput.value,
                    onValueChange = { amountInput.value = it },
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountInput.value.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        loanViewModel.updateLoanPrincipal(loan.id, -amount)
                    }
                    amountInput.value = ""
                    showPayDialog.value = false
                }) {
                    Text("Погасить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    amountInput.value = ""
                    showPayDialog.value = false
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun LoanDetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    val pattern = if (this % 1.0 == 0.0) "#,###" else "#,###.##"
    return DecimalFormat(pattern, sym).format(this)
}

private fun calculateMonthlyPayment(principal: Double, annualRate: Double, months: Int): Double {
    val monthlyRate = (annualRate / 100) / 12
    return if (monthlyRate == 0.0) {
        principal / months
    } else {
        principal * (monthlyRate * Math.pow(1 + monthlyRate, months.toDouble())) /
                (Math.pow(1 + monthlyRate, months.toDouble()) - 1)
    }
}