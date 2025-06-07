package com.kreditnik.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.ui.screens.PaymentScheduleItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScheduleScreen(
    loanViewModel: LoanViewModel,
    loanId: Long
) {
    val loans by loanViewModel.loans.collectAsState()
    val loan = loans.firstOrNull { it.id == loanId } ?: return
    val schedule = loanViewModel.calculatePaymentSchedule(loan)

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "График платежей",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                )
                Divider()
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(schedule) { item ->
                PaymentItem(item)
            }
        }
    }
}

@Composable
fun PaymentItem(item: PaymentScheduleItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${item.monthNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item.paymentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.totalPayment.formatMoney()} ₽",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Остаток: ${item.remainingPrincipal.formatMoney()} ₽",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text("Основной долг: ${item.principalPart.formatMoney()} ₽")
                Text("Проценты: ${item.interestPart.formatMoney()} ₽")
            }
        }
    }
}

private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    return DecimalFormat("#,##0.00", sym).format(this)
}
