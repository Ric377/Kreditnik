package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kreditnik.app.data.Operation
import com.kreditnik.app.viewmodel.LoanViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class SortOption { DATE, AMOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: LoanViewModel) {
    val operations by viewModel.operations.collectAsState()

    viewModel.loadOperations()

    var selectedSort by remember { mutableStateOf(SortOption.DATE) }
    var sortAscending by remember { mutableStateOf(false) }

    var expandedSort by remember { mutableStateOf(false) }

    val allLoans = viewModel.loans.collectAsState().value
    var selectedLoanId by remember { mutableStateOf<Long?>(null) }
    var expandedLoanFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "История операций",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    // Кнопка сортировки
                    IconButton(onClick = { expandedSort = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Сортировка")
                    }
                    DropdownMenu(
                        expanded = expandedSort,
                        onDismissRequest = { expandedSort = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Дата") },
                            onClick = {
                                selectedSort = SortOption.DATE
                                expandedSort = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сумма") },
                            onClick = {
                                selectedSort = SortOption.AMOUNT
                                expandedSort = false
                            }
                        )
                    }

                    // Кнопка фильтра по кредиту
                    IconButton(onClick = { expandedLoanFilter = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтр по кредиту")
                    }
                    DropdownMenu(
                        expanded = expandedLoanFilter,
                        onDismissRequest = { expandedLoanFilter = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все кредиты") },
                            onClick = {
                                selectedLoanId = null
                                expandedLoanFilter = false
                            }
                        )
                        allLoans.forEach { loan ->
                            DropdownMenuItem(
                                text = { Text(loan.name) },
                                onClick = {
                                    selectedLoanId = loan.id
                                    expandedLoanFilter = false
                                }
                            )
                        }
                    }

                    // Кнопка смены направления сортировки
                    IconButton(onClick = { sortAscending = !sortAscending }) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "Смена направления"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val filteredOperations = if (selectedLoanId != null) {
            operations.filter { it.loanId == selectedLoanId }
        } else {
            operations
        }

        val sortedOperations = when (selectedSort) {
            SortOption.DATE -> if (sortAscending) {
                filteredOperations.sortedBy { it.date }
            } else {
                filteredOperations.sortedByDescending { it.date }
            }
            SortOption.AMOUNT -> if (sortAscending) {
                filteredOperations.sortedBy { it.amount }
            } else {
                filteredOperations.sortedByDescending { it.amount }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            itemsIndexed(sortedOperations) { index, operation ->
                OperationItem(
                    operation = operation,
                    loanName = viewModel.getLoanNameById(operation.loanId)
                )
                if (index != sortedOperations.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

@Composable
fun OperationItem(operation: Operation, loanName: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp)
    ) {

    Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
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

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = loanName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = operation.date.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                String.format(Locale.US, "%+,.2f ₽", operation.amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (operation.amount < 0)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

