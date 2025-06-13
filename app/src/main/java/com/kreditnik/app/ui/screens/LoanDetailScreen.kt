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
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions
import java.util.*

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
                .padding(innerPadding)
                .padding(horizontal = 0.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                        "${calculateMonthlyPayment(loan.principal, loan.interestRate, loan.months).formatMoney()} $currency"
                    )
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
