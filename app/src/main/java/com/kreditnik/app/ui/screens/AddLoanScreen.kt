package com.kreditnik.app.ui.screens


import android.app.DatePickerDialog as AndroidDatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanType
import com.kreditnik.app.viewmodel.LoanViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    loanViewModel: LoanViewModel,
    navController: NavController,
    loan: Loan? = null
) {
    var name by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LoanType.CREDIT) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val fieldShape = RoundedCornerShape(16.dp)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Добавить новый кредит",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название кредита") },
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = principal,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() || it == '.' }) {
                        principal = newValue
                    }
                },
                label = { Text("Сумма кредита") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = interestRate,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() || it == '.' }) {
                        interestRate = newValue
                    }
                },
                label = { Text("Процентная ставка (%)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = months,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        months = newValue
                    }
                },
                label = { Text("Срок кредита (в месяцах)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- ПОЛЕ «Дата открытия» ----------
            val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

            Box(                                          // ① всё поле кликабельно
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null                 // без лишнего ripple
                    ) {
                        AndroidDatePickerDialog(          // системный календарь
                            context,
                            { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        ).show()
                    }
            ) {
                OutlinedTextField(
                    value         = selectedDate.format(dateFormatter),
                    onValueChange = {},                   // read-only
                    label         = { Text("Дата открытия") },
                    readOnly      = true,
                    enabled       = false,                // ② не перехватывает жест
                    trailingIcon  = { Icon(Icons.Filled.CalendarToday, null) },
                    shape         = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(   // оставляем «живые» цвета
                        disabledTextColor          = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor         = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor        = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor     = MaterialTheme.colorScheme.surface
                    ),
                    modifier      = Modifier.fillMaxWidth()
                )
            }




            Spacer(modifier = Modifier.height(12.dp))

            // Тип кредита
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedType.displayName,
                    onValueChange = {},
                    label = { Text("Тип кредита") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    shape = fieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    LoanType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val loanPrincipal = principal.toDoubleOrNull()
                    val loanMonths = months.toIntOrNull()
                    val loanInterestRate = interestRate.toDoubleOrNull()

                    if (name.isNotBlank() && loanPrincipal != null && loanMonths != null && loanInterestRate != null && loanPrincipal > 0 && loanMonths > 0) {
                        val loan = Loan(
                            name = name,
                            type = selectedType,
                            logo = "",
                            interestRate = loanInterestRate,
                            startDate = selectedDate,
                            monthlyPaymentDay = selectedDate.dayOfMonth,
                            principal = loanPrincipal,
                            months = loanMonths,
                            gracePeriodDays = null,
                            mandatoryPaymentDay = null,
                            gracePeriodEndDate = null,
                            debtDueDate = null
                        )
                        loanViewModel.addLoan(loan)
                        navController.popBackStack()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Пожалуйста, заполните все поля корректными данными.",
                                actionLabel = "ОК",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = fieldShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text("Сохранить", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}