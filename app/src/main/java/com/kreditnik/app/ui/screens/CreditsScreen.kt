@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kreditnik.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.kreditnik.app.R
import com.kreditnik.app.data.Loan
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.viewmodel.SettingsViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

/**
 * Форматирует число типа [Double] в денежную строку согласно российским стандартам.
 * - Использует пробел в качестве разделителя тысяч.
 * - Скрывает дробную часть, если она равна нулю.
 * @return Отформатированная строка.
 */
private fun Double.formatMoney(): String {
    val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' ' }
    val pattern = if (this % 1.0 == 0.0) "#,###" else "#,###.##"
    return DecimalFormat(pattern, sym).format(this)
}

/**
 * Внутреннее содержимое карточки кредита, отображающее иконку, название и сумму.
 * Этот компонент является stateless (не хранит состояние) и отвечает только за отображение.
 *
 * @param loan Объект кредита [Loan] для отображения.
 * @param currency Символ валюты для отображения рядом с суммой.
 */
@Composable
private fun LoanRowContent(loan: Loan, currency: String) {
    val totalAmountForDisplay = loan.principal + loan.accruedInterest

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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

        Spacer(Modifier.width(16.dp))

        Text(
            text = loan.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${totalAmountForDisplay.formatMoney()} $currency",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Карточка для отображения одного элемента в списке кредитов.
 * Поддерживает два состояния: обычное и выбранное. Реагирует на короткое и длинное нажатие.
 *
 * @param loan Отображаемый объект кредита [Loan].
 * @param currency Символ валюты.
 * @param isSelected `true`, если элемент в данный момент выбран (для визуального выделения).
 * @param onClick Лямбда, вызываемая при коротком нажатии.
 * @param onLongClick Лямбда, вызываемая при длинном нажатии (для активации режима выбора).
 */
@Composable
private fun LoanListItem(
    loan: Loan,
    currency: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        LoanRowContent(loan, currency)
    }
}

/**
 * Карточка для отображения мотивирующей цитаты в пейджере.
 *
 * @param text Текст цитаты для отображения.
 */
@Composable
private fun MotivationCard(text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(72.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


/**
 * Главный экран приложения, отображающий список всех кредитов.
 *
 * Экран имеет два режима работы:
 * 1.  **Обычный режим**: отображается общая сумма долга, список кредитов и пейджер
 * с мотивирующими цитатами. Нажатие на элемент открывает его детальный экран.
 * 2.  **Режим выбора**: активируется долгим нажатием на элемент. Верхняя панель (TopAppBar)
 * меняется на контекстную, позволяя выбрать все элементы, удалить выбранные или
 * сбросить выделение. Нажатие на кнопку "Назад" в этом режиме отменяет выбор.
 *
 * @param loanViewModel ViewModel для получения списка кредитов и их удаления.
 * @param settingsViewModel ViewModel для получения настроек, например, символа валюты.
 * @param navController Контроллер навигации для перехода на другие экраны.
 */
@Composable
fun CreditsScreen(
    loanViewModel: LoanViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavController
) {
    val loans by loanViewModel.loans.collectAsState()
    val currency by settingsViewModel.defaultCurrency.collectAsState()
    var selectedLoans by remember { mutableStateOf(setOf<Long>()) }

    val total by remember(loans) {
        derivedStateOf {
            loans.sumOf { it.principal + it.accruedInterest }.formatMoney()
        }
    }

    val context = LocalContext.current
    val motivationalQuotes = remember {
        context.resources.getStringArray(R.array.motivational_quotes).toList()
    }
    val pagerState = rememberPagerState(initialPage = 0)

    Scaffold(
        topBar = {
            if (selectedLoans.isEmpty()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Общая сумма: $total $currency",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selectedLoans = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить выделение")
                        }
                    },
                    title = { Text("${selectedLoans.size} выбрано") },
                    actions = {
                        IconButton(onClick = {
                            selectedLoans = loans.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                        }
                        IconButton(onClick = {
                            selectedLoans.forEach { id ->
                                loanViewModel.deleteLoan(loans.first { it.id == id })
                            }
                            selectedLoans = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить выбранные")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addLoan") }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить кредит")
            }
        }
    ) { innerPadding ->
        BackHandler(enabled = selectedLoans.isNotEmpty()) {
            selectedLoans = emptySet()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (motivationalQuotes.isNotEmpty()) {
                HorizontalPager(
                    count = motivationalQuotes.size,
                    state = pagerState,
                    modifier = Modifier.padding(top = 8.dp)
                ) { page ->
                    MotivationCard(text = motivationalQuotes[page])
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(loans, key = { it.id }) { loan ->
                    LoanListItem(
                        loan = loan,
                        currency = currency,
                        isSelected = loan.id in selectedLoans,
                        onClick = {
                            if (selectedLoans.isNotEmpty()) {
                                selectedLoans = if (loan.id in selectedLoans)
                                    selectedLoans - loan.id
                                else
                                    selectedLoans + loan.id
                            } else {
                                navController.navigate("loanDetail/${loan.id}")
                            }
                        },
                        onLongClick = {
                            selectedLoans = if (loan.id in selectedLoans)
                                selectedLoans - loan.id
                            else
                                selectedLoans + loan.id
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}