package com.kreditnik.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.data.OperationType
import com.kreditnik.app.data.Operation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.*
import java.time.LocalDate
import java.time.LocalDateTime


// модель одного пункта графика платежей
data class PaymentScheduleItem(
    val monthNumber: Int,
    val paymentDate: LocalDate,
    val totalPayment: Double,
    val principalPart: Double,
    val interestPart: Double,
    val remainingPrincipal: Double,
    val isPaid: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScheduleScreen(
    loanViewModel: LoanViewModel,
    loanId: Long
) {
    val loans by loanViewModel.loans.collectAsState()
    val loan = loans.firstOrNull { it.id == loanId } ?: return
    val operations by loanViewModel.operations.collectAsState()
    val baseSchedule = loanViewModel.calculatePaymentSchedule(loan)
// собираем даты платежей, которые уже есть в списке операций
    val paidDates = operations
        .filter { it.loanId == loanId && it.type == OperationType.PAYMENT && it.description == "Ежемесячный платёж" }
        .map    { it.date.toLocalDate() }
        .toSet()

// отмечаем в расписании, какие месяцы уже оплачены
    val monthlyPaidCount = operations
        .filter { it.loanId == loanId && it.type == OperationType.PAYMENT && it.description == "Ежемесячный платёж" }
        .size

    val schedule = baseSchedule.map { item ->
        item.copy(isPaid = item.monthNumber <= monthlyPaidCount)
    }



    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "График платежей",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)                    // отступ под AppBar
                .padding(horizontal = 16.dp, vertical = 8.dp), // как в HistoryScreen
            // spacing между карточками будет через Spacer(4.dp)
        ) {
            items(schedule) { item ->
                PaymentItem(item)
                Spacer(modifier = Modifier.height(4.dp)) // точно как в истории операций
            }
            item {
                TotalSummary(schedule)
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
            .padding(horizontal = 0.dp, vertical = 4.dp) // mirror OperationItem external padding
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Номер в кружке + дата
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.monthNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item.paymentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Платеж + остаток
                Column(horizontalAlignment = Alignment.End) {
                    if (item.isPaid) {
                        Text(
                            text = "Погашено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = "${item.totalPayment.formatMoney()} ₽",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Остаток: ${item.remainingPrincipal.formatMoney()} ₽",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Основной долг: ${item.principalPart.formatMoney()} ₽",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Проценты: ${item.interestPart.formatMoney()} ₽",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


@Composable
fun TotalSummary(schedule: List<PaymentScheduleItem>) {
    val totalPrincipal = schedule.sumOf { it.principalPart }
    val totalInterest = schedule.sumOf { it.interestPart }
    val totalPayment = totalPrincipal + totalInterest

    Spacer(Modifier.height(4.dp)) // как между карточками

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // mirror item padding
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Итого",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Основной долг: ${totalPrincipal.formatMoney()} ₽",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Проценты: ${totalInterest.formatMoney()} ₽",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Всего: ${totalPayment.formatMoney()} ₽",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    return DecimalFormat("#,##0.00", sym).format(this)
}
