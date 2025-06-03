@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.viewmodel.SettingsViewModel   // <-- добавили импорт
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import androidx.compose.material3.CenterAlignedTopAppBar
import java.util.*

/* ---------- форматируем 1 234 567 ---------- */
private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    return DecimalFormat("#,###", sym).format(this)
}

/* ---------- содержимое строки ---------- */
@Composable
private fun LoanRowContent(loan: Loan, currency: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = .15f),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Filled.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = loan.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${loan.principal.formatMoney()} $currency",    // <-- Используем валюту
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/* ---------- полупрозрачная карточка ---------- */
@Composable
private fun LoanListItem(
    loan: Loan,
    currency: String,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) { LoanRowContent(loan, currency) }
}

/* ---------- главный экран ---------- */
@Composable
fun CreditsScreen(
    loanViewModel: LoanViewModel,
    settingsViewModel: SettingsViewModel,  // <-- добавили
    navController: NavController
) {
    val loans by loanViewModel.loans.collectAsState()
    val currency by settingsViewModel.defaultCurrency.collectAsState() // <-- вытаскиваем валюту

    /* общая сумма обновляется при любом изменении списка */
    val total by remember(loans) {
        derivedStateOf { loans.sumOf { it.principal }.formatMoney() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text  = "Общая сумма: $total $currency",    // <-- показываем валюту
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addLoan") }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить кредит")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(loans, key = { it.id }) { loan ->
                LoanListItem(loan, currency)  // <-- передаем валюту в карточку
            }
        }
    }
}
