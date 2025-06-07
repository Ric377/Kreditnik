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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Форматируем сумму: "1 234 567" или "1 234 567.89"
private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    val pattern = if (this % 1.0 == 0.0) "#,###" else "#,###.##"
    return DecimalFormat(pattern, sym).format(this)
}

/** Контент одной строки списка кредитов */
@Composable
private fun LoanRowContent(loan: Loan, currency: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp) // точно как в HistoryScreen
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${loan.principal.formatMoney()} $currency",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Одна карточка-кредит */
@Composable
private fun LoanListItem(
    loan: Loan,
    currency: String,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp), // как в операциях
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp) // vertical = 4.dp совпадает с HistoryScreen
    ) {
        LoanRowContent(loan, currency)
    }
}

/** Экран "Кредиты" */
@Composable
fun CreditsScreen(
    loanViewModel: LoanViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavController
) {
    val loans by loanViewModel.loans.collectAsState()
    val currency by settingsViewModel.defaultCurrency.collectAsState()

    val total by remember(loans) {
        derivedStateOf { loans.sumOf { it.principal }.formatMoney() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Общая сумма: $total $currency",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addLoan") }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить кредит")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)                 // отступ под AppBar и над NavigationBar
                .padding(horizontal = 0.dp, vertical = 8.dp), // vertical = 8.dp как в HistoryScreen
            contentPadding = PaddingValues() // горизонтальных pad'ов здесь не нужно
        ) {
            items(loans, key = { it.id }) { loan ->
                LoanListItem(loan, currency) {
                    navController.navigate("loanDetail/${loan.id}")
                }
                Spacer(modifier = Modifier.height(4.dp)) // как в истории: разделитель через 4.dp
            }
        }
    }
}
