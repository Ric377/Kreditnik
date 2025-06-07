package com.kreditnik.app.ui.screens

import androidx.compose.runtime.Composable
import com.kreditnik.app.ui.components.CenteredText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kreditnik.app.data.Operation
import com.kreditnik.app.viewmodel.LoanViewModel

@Composable
fun HistoryScreen(viewModel: LoanViewModel) {
    val operations by viewModel.operations.collectAsState()

    viewModel.loadOperations()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(operations) { operation ->
            OperationItem(operation)
        }
    }
}

@Composable
fun OperationItem(operation: Operation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = operation.type.name, style = MaterialTheme.typography.titleMedium)
        Text(text = "${operation.amount} ₽", style = MaterialTheme.typography.bodyLarge)
        Text(text = operation.date.toString(), style = MaterialTheme.typography.bodySmall)
        operation.description?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
}