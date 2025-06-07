package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.viewmodel.SettingsViewModel
import com.kreditnik.app.viewmodel.LoanViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import com.kreditnik.app.data.Operation
import com.kreditnik.app.data.OperationType
import java.time.LocalDateTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem


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
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню действий")
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
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LoanDetailItem("Тип кредита", loan.type.displayName)
                    LoanDetailItem("Сумма", "${loan.principal.formatMoney()} $currency")
                    LoanDetailItem("Процентная ставка", "${loan.interestRate}%")
                    LoanDetailItem("Срок", "${loan.months} месяцев")
                    LoanDetailItem(
                        "Дата открытия",
                        loan.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    )
                    LoanDetailItem(
                        "Ежемесячный платёж",
                        "${calculateMonthlyPayment(loan.principal, loan.interestRate, loan.months)
                            .formatMoney()} $currency"
                    )
                    LoanDetailItem(
                        "Ближайший платёж",
                        getNextPaymentDate(loan.startDate, loan.monthlyPaymentDay)
                            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { showAddDialog.value = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить долг")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить долг")
                }

                Button(
                    onClick = { showPayDialog.value = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary  // ❗ тоже primary
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Погасить долг")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Погасить долг")
                }
            }
        }
    }

    // 🔥 Диалоги В КОНЦЕ ФУНКЦИИ, после Scaffold

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
                TextButton(
                    onClick = {
                        val amount = amountInput.value.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            loanViewModel.addOperation(
                                Operation(
                                    loanId = loan.id,
                                    amount = amount,
                                    date = LocalDateTime.now(),
                                    type = OperationType.OTHER,
                                    description = "Добавление долга"
                                )
                            )
                        }
                        amountInput.value = ""
                        showAddDialog.value = false
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog.value = false
                    amountInput.value = ""
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
                TextButton(
                    onClick = {
                        val amount = amountInput.value.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            loanViewModel.addOperation(
                                Operation(
                                    loanId = loan.id,
                                    amount = -amount,
                                    date = LocalDateTime.now(),
                                    type = OperationType.PAYMENT,
                                    description = "Погашение долга"
                                )
                            )
                        }
                        amountInput.value = ""
                        showPayDialog.value = false
                    }
                ) {
                    Text("Погасить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPayDialog.value = false
                    amountInput.value = ""
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

// ====== Здесь заменили функцию форматирования ======
private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    // Если дробная часть = 0, показываем без десятичных. Иначе – до двух знаков после точки.
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

private fun getNextPaymentDate(startDate: LocalDate, paymentDay: Int): LocalDate {
    val today = LocalDate.now()

    // Если paymentDay == 0, считаем за «последний день месяца»
    val dayThisMonth = if (paymentDay == 0) today.lengthOfMonth()
    else paymentDay.coerceAtMost(today.lengthOfMonth())
    val thisMonthPayment = today.withDayOfMonth(dayThisMonth)

    return if (today <= thisMonthPayment) {
        thisMonthPayment
    } else {
        val nextMonth = today.plusMonths(1)
        val dayNextMonth = if (paymentDay == 0) nextMonth.lengthOfMonth()
        else paymentDay.coerceAtMost(nextMonth.lengthOfMonth())
        nextMonth.withDayOfMonth(dayNextMonth)
    }
}
