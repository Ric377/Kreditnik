package com.kreditnik.app.ui.screens

import android.app.DatePickerDialog as AndroidDatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanType
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.data.DayCountConvention
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text

/**
 * Экран для добавления нового или редактирования существующего кредита/займа.
 *
 * Этот Composable представляет собой форму с полями для ввода всех необходимых
 * параметров кредита, таких как название, сумма, процентная ставка, дата начала и т. д.
 * Экран работает в двух режимах:
 * 1.  **Режим добавления**: Если в параметр `loan` передаётся `null`, поля будут пустыми,
 * и по нажатию кнопки "Сохранить" будет создан новый объект `Loan`.
 * 2.  **Режим редактирования**: Если передать существующий объект `Loan`, форма будет
 * предзаполнена его данными, а кнопка изменит название на "Сохранить изменения".
 *
 * Ключевые особенности:
 * - Валидация вводимых данных с подсветкой некорректно заполненных полей.
 * - Автоматический расчёт срока кредита в месяцах на основе суммы, ставки и ежемесячного платежа.
 * - Выбор типа кредита (кредит или займ) и дня ежемесячного платежа.
 * - Возможность использовать специфичный метод расчёта процентов (по методу Сбера).
 * - Взаимодействие с [LoanViewModel] для сохранения или обновления данных в репозитории.
 * - Использование [NavController] для возврата на предыдущий экран после успешного сохранения.
 *
 * @param loanViewModel ViewModel для управления данными о кредитах.
 * @param navController Контроллер навигации для управления перемещением между экранами.
 * @param loan Необязательный объект [Loan]. Если он не `null`, экран переходит в режим редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    loanViewModel: LoanViewModel,
    navController: NavController,
    loan: Loan? = null
) {
    val settingsVM: com.kreditnik.app.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    var name by remember { mutableStateOf(loan?.name ?: "") }
    var principal by remember { mutableStateOf(loan?.initialPrincipal?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(loan?.interestRate?.toString() ?: "") }
    var months by remember { mutableStateOf(loan?.months?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(loan?.type ?: LoanType.CREDIT) }
    var selectedDate by remember { mutableStateOf(loan?.startDate ?: LocalDate.now()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val daysInMonth = (1..28).toList() + listOf(0)  // 0 — «Последний день месяца»
    var selectedPaymentDay by remember { mutableStateOf(loan?.monthlyPaymentDay ?: selectedDate.dayOfMonth) }
    var paymentDayExpanded by remember { mutableStateOf(false) }

    var manualMonthlyPayment by remember { mutableStateOf(loan?.monthlyPayment?.toString() ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var principalError by remember { mutableStateOf(false) }
    var interestRateError by remember { mutableStateOf(false) }
    var monthsError by remember { mutableStateOf(false) }
    var monthlyPaymentError by remember { mutableStateOf(false) }

    var useSberbankMethod by remember { mutableStateOf(loan?.usesSberbankCalculation ?: false) }

    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val fieldShape = RoundedCornerShape(16.dp)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (loan == null) "Добавить кредит" else "Редактировать кредит",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                isError = nameError,
                label = { Text("Название кредита") },
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            if (nameError) {
                Text(
                    text = "Введите название кредита",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedType.displayName,
                    onValueChange = {},
                    label = { Text("Тип кредита") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    shape = fieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    LoanType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = principal,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() || it == '.' }) {
                        principal = newValue
                        principalError = false
                    }
                },
                isError = principalError,
                label = { Text("Сумма кредита") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            if (principalError) {
                Text(
                    text = "Введите сумму больше 0",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            OutlinedTextField(
                value = interestRate,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() || it == '.' }) {
                        interestRate = newValue
                        interestRateError = false
                    }
                },
                isError = interestRateError,
                label = { Text("Процентная ставка (%)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            if (interestRateError) {
                Text(
                    text = "Введите процентную ставку",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useSberbankMethod = !useSberbankMethod }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useSberbankMethod,
                    onCheckedChange = { useSberbankMethod = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Расчёт начисленных процентов по методу Сбера",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        AndroidDatePickerDialog(
                            context,
                            { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        ).show()
                    }
            ) {
                OutlinedTextField(
                    value = selectedDate.format(dateFormatter),
                    onValueChange = {},
                    label = { Text("Дата открытия") },
                    readOnly = true,
                    enabled = false,
                    trailingIcon = { Icon(Icons.Filled.CalendarToday, null) },
                    shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ExposedDropdownMenuBox(
                expanded = paymentDayExpanded,
                onExpandedChange = { paymentDayExpanded = !paymentDayExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = if (selectedPaymentDay == 0) "Последний день месяца" else "$selectedPaymentDay",
                    onValueChange = {},
                    label = { Text("День платежа") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentDayExpanded) },
                    shape = fieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors()
                )
                ExposedDropdownMenu(
                    expanded = paymentDayExpanded,
                    onDismissRequest = { paymentDayExpanded = false }
                ) {
                    daysInMonth.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(if (day == 0) "Последний день месяца" else "$day") },
                            onClick = {
                                selectedPaymentDay = day
                                paymentDayExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = manualMonthlyPayment,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() || it == '.' }) {
                        manualMonthlyPayment = newValue
                        monthlyPaymentError = false
                    }
                },
                isError = monthlyPaymentError,
                label = { Text("Ежемесячный платёж") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            if (monthlyPaymentError) {
                Text(
                    text = "Введите корректный платёж",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            OutlinedTextField(
                value = months,
                onValueChange = {},
                label = { Text("Срок в месяцах") },
                readOnly = true,
                isError = monthsError,
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            if (monthsError) {
                Text(
                    text = "Введите срок больше 0",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            LaunchedEffect(principal, interestRate, manualMonthlyPayment) {
                val loanPrincipal = principal.toDoubleOrNull()
                val loanInterestRate = interestRate.toDoubleOrNull()
                val payment = manualMonthlyPayment.toDoubleOrNull()

                if (loanPrincipal == null || loanInterestRate == null || payment == null || payment <= 0) {
                    months = ""
                    return@LaunchedEffect
                }

                val monthlyRate = (loanInterestRate / 100) / 12

                if (monthlyRate > 0 && payment <= loanPrincipal * monthlyRate) {
                    months = "∞"
                    return@LaunchedEffect
                }

                val calculatedMonths = if (monthlyRate == 0.0) {
                    loanPrincipal / payment
                } else {
                    -Math.log(1 - (loanPrincipal * monthlyRate / payment)) / Math.log(1 + monthlyRate)
                }

                months = kotlin.math.ceil(calculatedMonths).toInt().toString()
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val loanPrincipal = principal.toDoubleOrNull()
                    val loanMonths = months.toIntOrNull()
                    val loanInterestRate = interestRate.toDoubleOrNull()
                    val finalMonthlyPayment = manualMonthlyPayment.toDoubleOrNull()

                    nameError = name.isBlank()
                    principalError = loanPrincipal == null || loanPrincipal <= 0
                    interestRateError = loanInterestRate == null
                    monthsError = months.isBlank() || months == "∞"
                    monthlyPaymentError = finalMonthlyPayment == null || finalMonthlyPayment <= 0

                    val hasError = nameError || principalError || interestRateError || monthsError || monthlyPaymentError

                    if (!hasError) {
                        scope.launch {
                            if (loan == null) {
                                val newLoan = Loan(
                                    name              = name,
                                    type              = selectedType,
                                    logo              = "",
                                    interestRate      = loanInterestRate!!,
                                    startDate         = selectedDate,
                                    monthlyPaymentDay = selectedPaymentDay,
                                    initialPrincipal  = loanPrincipal!!,
                                    principal         = loanPrincipal,
                                    months            = loanMonths!!,
                                    monthlyPayment    = finalMonthlyPayment!!,
                                    usesSberbankCalculation = useSberbankMethod,
                                    gracePeriodDays   = null,
                                    mandatoryPaymentDay = null,
                                    gracePeriodEndDate  = null,
                                    debtDueDate         = null,
                                    dayCountConvention  = DayCountConvention.RETAIL,
                                    reminderDaysBefore  = settingsVM.reminderDaysBefore.value,
                                    reminderTime        = settingsVM.reminderTime.value
                                )
                                loanViewModel.addLoan(newLoan)

                            } else {
                                val updatedLoan = if (selectedDate != loan.startDate) {
                                    loan.copy(
                                        name              = name,
                                        type              = selectedType,
                                        interestRate      = loanInterestRate!!,
                                        startDate         = selectedDate,
                                        monthlyPaymentDay = selectedPaymentDay,
                                        initialPrincipal  = loanPrincipal!!,
                                        monthlyPayment    = finalMonthlyPayment!!,
                                        usesSberbankCalculation = useSberbankMethod,
                                        months            = loanMonths!!,
                                        principal         = loanPrincipal,
                                        accruedInterest   = 0.0,
                                        lastInterestCalculationDate = selectedDate
                                    )
                                } else {
                                    loan.copy(
                                        name              = name,
                                        type              = selectedType,
                                        interestRate      = loanInterestRate!!,
                                        startDate         = selectedDate,
                                        monthlyPaymentDay = selectedPaymentDay,
                                        initialPrincipal  = loanPrincipal!!,
                                        monthlyPayment    = finalMonthlyPayment!!,
                                        usesSberbankCalculation = useSberbankMethod,
                                        months            = loanMonths!!
                                    )
                                }
                                loanViewModel.updateLoan(updatedLoan)
                            }
                            navController.popBackStack()
                        }
                    }
                },
            ) {
                Text(
                    text = if (loan == null) "Сохранить" else "Сохранить изменения",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}