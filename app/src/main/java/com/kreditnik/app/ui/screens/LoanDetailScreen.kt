package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.viewmodel.SettingsViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import com.kreditnik.app.viewmodel.LoanViewModel


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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loan.name) },
                actions = {
                    IconButton(onClick = { expandedMenu.value = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Меню действий")
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // TODO: Логика редактирования
            }) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
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
                    LoanDetailItem("Дата открытия", loan.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                    LoanDetailItem("Ежемесячный платёж", "${calculateMonthlyPayment(loan).formatMoney()} $currency")
                    LoanDetailItem("День платежа", "${loan.monthlyPaymentDay}-е число")
                }
            }
        }
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
    return DecimalFormat("#,###", sym).format(this)
}

private fun calculateMonthlyPayment(loan: Loan): Double {
    val principal = loan.principal
    val rate = loan.interestRate / 100 / 12
    val months = loan.months
    return if (rate == 0.0) {
        principal / months
    } else {
        val factor = Math.pow(1 + rate, months.toDouble())
        principal * rate * factor / (factor - 1)
    }
}
