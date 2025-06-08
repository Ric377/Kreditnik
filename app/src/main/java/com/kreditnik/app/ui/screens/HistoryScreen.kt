package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.rememberDismissState

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kreditnik.app.data.Operation
import com.kreditnik.app.viewmodel.LoanViewModel
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.Locale

enum class SortOption { DATE, AMOUNT }

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HistoryScreen(viewModel: LoanViewModel) {
    val operations by viewModel.operations.collectAsState()
    viewModel.loadOperations()

    var selectedSort        by remember { mutableStateOf(SortOption.DATE) }
    var sortAscending       by remember { mutableStateOf(false) }
    var expandedSort        by remember { mutableStateOf(false) }
    var selectedLoanId      by remember { mutableStateOf<Long?>(null) }
    var expandedLoanFilter  by remember { mutableStateOf(false) }
    var selectedOperations  by remember { mutableStateOf(setOf<Long>()) }
    var editingOperation    by remember { mutableStateOf<Operation?>(null) }

    val allLoans = viewModel.loans.collectAsState().value

    // Фильтрация и сортировка
    val filteredOperations = if (selectedLoanId != null)
        operations.filter { it.loanId == selectedLoanId }
    else
        operations

    val sortedOperations = when (selectedSort) {
        SortOption.DATE   -> if (sortAscending) filteredOperations.sortedBy { it.date }
        else               filteredOperations.sortedByDescending { it.date }
        SortOption.AMOUNT -> if (sortAscending) filteredOperations.sortedBy { it.amount }
        else               filteredOperations.sortedByDescending { it.amount }
    }

    Scaffold(
        topBar = {
            if (selectedOperations.isEmpty()) {
                CenterAlignedTopAppBar(
                    title = { Text("История операций", style = MaterialTheme.typography.headlineSmall) },
                    actions = {
                        IconButton(onClick = { expandedSort = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Сортировка")
                        }
                        DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                            DropdownMenuItem(text = { Text("Дата") },   onClick = { selectedSort = SortOption.DATE;   expandedSort = false })
                            DropdownMenuItem(text = { Text("Сумма") }, onClick = { selectedSort = SortOption.AMOUNT; expandedSort = false })
                        }
                        IconButton(onClick = { expandedLoanFilter = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Фильтр по кредиту")
                        }
                        DropdownMenu(expanded = expandedLoanFilter, onDismissRequest = { expandedLoanFilter = false }) {
                            DropdownMenuItem(text = { Text("Все кредиты") }, onClick = { selectedLoanId = null; expandedLoanFilter = false })
                            allLoans.forEach { loan ->
                                DropdownMenuItem(text = { Text(loan.name) }, onClick = { selectedLoanId = loan.id; expandedLoanFilter = false })
                            }
                        }
                        IconButton(onClick = { sortAscending = !sortAscending }) {
                            Icon(
                                imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Смена направления"
                            )
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selectedOperations = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить выделение")
                        }
                    },
                    title = { Text("${selectedOperations.size}") },
                    actions = {
                        IconButton(onClick = { selectedOperations = sortedOperations.map { it.id }.toSet() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                        }
                        IconButton(onClick = {
                            selectedOperations.forEach { id ->
                                viewModel.deleteOperation(operations.first { it.id == id })
                            }
                            selectedOperations = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить выделенные")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(sortedOperations, key = { it.id }) { operation ->
                var showMenu by remember { mutableStateOf(false) }

                val swipeEnabled = selectedOperations.isEmpty()

                val dismissState = rememberDismissState(
                    confirmStateChange = { state ->
                        if (state == DismissValue.DismissedToStart) {
                            if (swipeEnabled) {
                                showMenu = true
                            }
                            false
                        } else {
                            true
                        }
                    }
                )

                SwipeToDismiss(
                    state = dismissState,
                    directions = if (swipeEnabled) setOf(DismissDirection.EndToStart) else emptySet(),
                    background = {
                        if (swipeEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 24.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(32.dp))
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    dismissContent = {
                        OperationItem(
                            operation = operation,
                            loanName = viewModel.getLoanNameById(operation.loanId),
                            isSelected = operation.id in selectedOperations,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (selectedOperations.isNotEmpty()) {
                                        selectedOperations = if (operation.id in selectedOperations)
                                            selectedOperations - operation.id
                                        else
                                            selectedOperations + operation.id
                                    }
                                },
                                onLongClick = {
                                    if (selectedOperations.isEmpty()) {
                                        selectedOperations = setOf(operation.id)
                                    } else {
                                        selectedOperations = if (operation.id in selectedOperations)
                                            selectedOperations - operation.id
                                        else
                                            selectedOperations + operation.id
                                    }
                                }
                            )
                        )
                    }
                )

                if (showMenu) {
                    AlertDialog(
                        onDismissRequest = { showMenu = false },
                        title = { Text("Действие с операцией") },
                        confirmButton = {
                            TextButton(onClick = {
                                showMenu = false
                                editingOperation = operation
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Редактировать")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showMenu = false
                                viewModel.deleteOperation(operation)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Удалить")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))


            }
        }

        editingOperation?.let { op ->
            EditOperationDialog(
                op,
                onDismiss = { editingOperation = null },
                onSave    = {
                    viewModel.updateOperation(it)
                    editingOperation = null
                }
            )
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

@Composable
fun OperationItem(
    operation: Operation,
    loanName: String,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = loanName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = operation.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                String.format(Locale.US, "%+,.2f ₽", operation.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (operation.amount < 0)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun EditOperationDialog(
    op: Operation,
    onDismiss: () -> Unit,
    onSave: (Operation) -> Unit
) {
    var amount by remember { mutableStateOf(op.amount.toString()) }
    var desc   by remember { mutableStateOf(op.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать операцию") },
        text  = {
            Column {
                OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") })
                OutlinedTextField(desc,   { desc   = it }, label = { Text("Описание") })
            }
        },
        confirmButton = {
            TextButton({
                onSave(op.copy(
                    amount      = amount.toDoubleOrNull() ?: op.amount,
                    description = desc
                ))
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}
