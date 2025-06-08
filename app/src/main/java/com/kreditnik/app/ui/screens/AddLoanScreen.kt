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
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    loanViewModel: LoanViewModel,
    navController: NavController,
    loan: Loan? = null
) {
    var selectedConvention by remember { mutableStateOf(loan?.dayCountConvention ?: DayCountConvention.SBER) }

    // ==== Переменные состояния ====
    var name by remember { mutableStateOf(loan?.name ?: "") }
    var principal by remember { mutableStateOf(loan?.principal?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(loan?.interestRate?.toString() ?: "") }
    var months by remember { mutableStateOf(loan?.months?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(loan?.type ?: LoanType.CREDIT) }
    var selectedDate by remember { mutableStateOf(loan?.startDate ?: LocalDate.now()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val daysInMonth = (1..28).toList() + listOf(0)  // 0 — «Последний день месяца»
    var selectedPaymentDay by remember { mutableStateOf(loan?.monthlyPaymentDay ?: selectedDate.dayOfMonth) }
    var paymentDayExpanded by remember { mutableStateOf(false) }

    var manualMonthlyPayment by remember { mutableStateOf("") }
    var autoCalculatePayment by remember { mutableStateOf(false) }

    // Ошибки для подсветки
    var nameError by remember { mutableStateOf(false) }
    var principalError by remember { mutableStateOf(false) }
    var interestRateError by remember { mutableStateOf(false) }
    var monthsError by remember { mutableStateOf(false) }
    var monthlyPaymentError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val fieldShape = RoundedCornerShape(16.dp)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ==== UI ====
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
                .padding(horizontal = 16.dp, vertical = 8.dp) // Везде одинаковые отступы
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Тоже одинаково
        ) {
            // Здесь начинается форма: OutlinedTextField, DropdownMenu и так далее

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
                    modifier = Modifier.padding(start = 16.dp) // отступ под «иконку» поля
                )
            }

            // ==== БЛОК 2: Тип кредита ====
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

            // ==== БЛОК 3: Сумма кредита ====
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

            // ==== БЛОК 4: Процентная ставка (%) ====
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

            // ==== БЛОК 5: Срок кредита (в месяцах) ====
            OutlinedTextField(
                value = months,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        months = newValue
                        monthsError = false
                    }
                },
                isError = monthsError,
                label = { Text("Срок кредита (в месяцах)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
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

            // ==== БЛОК 6: Дата открытия ====
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

            // ==== БЛОК 7: День платежа ====
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

            // ==== БЛОК 8: Ежемесячный платёж (с опцией Auto) ====
            OutlinedTextField(
                value = manualMonthlyPayment,
                onValueChange = {
                    if (!autoCalculatePayment && it.all { ch -> ch.isDigit() || ch == '.' }) {
                        manualMonthlyPayment = it
                        monthlyPaymentError = false
                    }
                },
                isError = monthlyPaymentError,
                label = { Text("Ежемесячный платёж") },
                readOnly = autoCalculatePayment,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = autoCalculatePayment,
                            onCheckedChange = { autoCalculatePayment = it }
                        )
                    }
                },
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth()
            )
            if (autoCalculatePayment) {
                val conventionLabels = mapOf(
                    DayCountConvention.RETAIL to "Стандартный",
                    DayCountConvention.SBER   to "Ежедневный (Сбербанк)"
                )
                var conventionExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = conventionExpanded,
                    onExpandedChange = { conventionExpanded = !conventionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = conventionLabels[selectedConvention]!!,
                        onValueChange = {},
                        label = { Text("Метод расчёта") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conventionExpanded) },
                        shape = fieldShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = conventionExpanded,
                        onDismissRequest = { conventionExpanded = false }
                    ) {
                        DayCountConvention.values().forEach { conv ->
                            DropdownMenuItem(
                                text = { Text(conventionLabels[conv]!!) },
                                onClick = {
                                    selectedConvention = conv
                                    conventionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (monthlyPaymentError) {
                Text(
                    text = "Введите корректный платёж",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // ==== Авторасчёт ежемесячного платежа ====
            LaunchedEffect(autoCalculatePayment, principal, interestRate, months) {
                if (autoCalculatePayment) {
                    val loanPrincipal = principal.toDoubleOrNull()
                    val loanMonths = months.toIntOrNull()
                    val loanInterestRate = interestRate.toDoubleOrNull()
                    if (loanPrincipal != null && loanMonths != null && loanInterestRate != null && loanMonths > 0) {
                        val monthlyRate = (loanInterestRate / 100) / 12
                        // если ставка нулевая, просто делим
                        val calculatedPayment = if (monthlyRate == 0.0) {
                            loanPrincipal / loanMonths
                        } else {
                            loanPrincipal * (monthlyRate * Math.pow(1 + monthlyRate, loanMonths.toDouble())) /
                                    (Math.pow(1 + monthlyRate, loanMonths.toDouble()) - 1)
                        }
                        // заполняем строку двумя знаками после запятой
                        manualMonthlyPayment = String.format("%.2f", calculatedPayment)
                    }
                }
            }

            // ==== БЛОК 9: Кнопка «Сохранить» с валидацией ====
            Button(
                onClick = {
                    val loanPrincipal = principal.toDoubleOrNull()
                    val loanMonths = months.toIntOrNull()
                    val loanInterestRate = interestRate.toDoubleOrNull()

                    val monthlyPayment = if (autoCalculatePayment) {
                        if (loanPrincipal != null && loanMonths != null && loanInterestRate != null && loanMonths > 0) {
                            val monthlyRate = (loanInterestRate / 100) / 12
                            if (monthlyRate == 0.0) {
                                loanPrincipal / loanMonths
                            } else {
                                loanPrincipal * (monthlyRate * Math.pow(1 + monthlyRate, loanMonths.toDouble())) /
                                        (Math.pow(1 + monthlyRate, loanMonths.toDouble()) - 1)
                            }
                        } else {
                            null
                        }
                    } else {
                        manualMonthlyPayment.toDoubleOrNull()
                    }

                    nameError = name.isBlank()
                    principalError = loanPrincipal == null || loanPrincipal <= 0
                    interestRateError = loanInterestRate == null
                    monthsError = loanMonths == null || loanMonths <= 0
                    monthlyPaymentError = monthlyPayment == null

                    val hasError = nameError || principalError || interestRateError || monthsError || monthlyPaymentError

                    if (!hasError) {
                        scope.launch {
                            if (loan == null) {
                                val newLoan = Loan(
                                    name = name,
                                    type = selectedType,
                                    logo = "",
                                    interestRate = loanInterestRate!!,
                                    startDate = selectedDate,
                                    monthlyPaymentDay = selectedPaymentDay,
                                    principal = loanPrincipal!!,
                                    months = loanMonths!!,
                                    gracePeriodDays = null,
                                    mandatoryPaymentDay = null,
                                    gracePeriodEndDate = null,
                                    debtDueDate = null,
                                    dayCountConvention = selectedConvention
                                )
                                loanViewModel.addLoan(newLoan)
                                loanViewModel.loadLoans() // <----- ДОБАВИЛ ЭТО
                            } else {
                                val updatedLoan = loan.copy(
                                    name = name,
                                    type = selectedType,
                                    interestRate = loanInterestRate!!,
                                    startDate = selectedDate,
                                    monthlyPaymentDay = selectedPaymentDay,
                                    principal = loanPrincipal!!,
                                    months = loanMonths!!,
                                    dayCountConvention = selectedConvention
                                )
                                loanViewModel.updateLoan(updatedLoan)
                                loanViewModel.loadLoans() // <----- ДОБАВИЛ ЭТО
                            }
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = if (loan == null) "Сохранить" else "Сохранить изменения",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
