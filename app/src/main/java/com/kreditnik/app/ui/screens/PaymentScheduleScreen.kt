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
import androidx.compose.ui.unit.dp
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.ui.screens.PaymentScheduleItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PaymentScheduleScreen(
    loanViewModel: LoanViewModel,
    loanId: Long
) {
    val loans by loanViewModel.loans.collectAsState()
    val loan = loans.firstOrNull { it.id == loanId } ?: return
    val schedule = loanViewModel.calculatePaymentSchedule(loan)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(schedule) { item ->
            PaymentItem(item)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#${item.monthNumber}")
                Column(horizontalAlignment = Alignment.End) {
                    Text("${item.totalPayment.formatMoney()} ₽")
                    Text(
                        item.paymentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Остаток: ${item.remainingPrincipal.formatMoney()} ₽",
                        style = MaterialTheme.typography.labelSmall
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
