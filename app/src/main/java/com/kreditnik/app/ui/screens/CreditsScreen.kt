package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kreditnik.app.viewmodel.LoanViewModel
import java.time.format.DateTimeFormatter

@Composable
fun CreditsScreen(loanViewModel: LoanViewModel, navController: NavController) {
    val loans by loanViewModel.loans.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addLoan") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить кредит")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(loans) { loan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(loan.name, style = MaterialTheme.typography.titleMedium)
                        Text("Сумма: ${loan.principal} ₽", style = MaterialTheme.typography.bodyMedium)
                        Text("Процент: ${loan.interestRate}%", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Дата открытия: ${
                                loan.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
