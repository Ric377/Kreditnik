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
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.text.font.FontWeight





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

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

@Composable
fun OperationItem(operation: Operation) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка операции
            Icon(
                imageVector = if (operation.amount < 0)
                    Icons.Filled.Remove else Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Текст
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = operation.description ?: "Без описания",
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

            // Сумма
            Text(
                text = String.format(
                    "%+,.2f ₽", operation.amount
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (operation.amount > 0)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}
