/*  app/src/main/java/com/kreditnik/app/ui/screens/CreditsScreen.kt  */
package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/* ---------- форматируем сумму вида 1 234 567 ₽ ---------- */
private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    return DecimalFormat("#,###", sym).format(this)
}

/* ---------- контент одной строки (логотип-название-сумма) ---------- */
@Composable
private fun LoanRowContent(loan: Loan) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        /* логотип-заглушка */
        Surface(
            shape  = CircleShape,
            color  = MaterialTheme.colorScheme.primary.copy(alpha = .15f),
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

        /* название */
        Text(
            text = loan.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        /* сумма справа */
        Text(
            text = "${loan.principal.formatMoney()} ₽",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/* ---------- сама карточка ─ полупрозрачный вариант ---------- */
@Composable
private fun LoanListItem(
    loan: Loan,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(12.dp),
        colors  = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) // 40 % прозрачности
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LoanRowContent(loan)
    }
}

/* ---------- главный экран со списком ---------- */
@Composable
fun CreditsScreen(
    loanViewModel: LoanViewModel,
    navController: NavController
) {
    val loans by loanViewModel.loans.collectAsState()

    Scaffold(
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
                LoanListItem(loan)   // по клику можно добавить навигацию
            }
        }
    }
}
