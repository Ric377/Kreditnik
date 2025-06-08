package com.kreditnik.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kreditnik.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val motivationalQuotes = remember {
        context.resources.getStringArray(R.array.motivational_quotes).toList()
    }

    val pagerState = rememberPagerState(initialPage = 0)

    val paidLoans = 3
    val remainingLoans = 2

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Аналитика",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // МЕНЬШЕ расстояние
        ) {
            Text(
                text = "Всего кредитов: ${paidLoans + remainingLoans}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Сумма задолженности: 250 000 ₽",
                style = MaterialTheme.typography.bodyLarge
            )

            Divider()

            Text(
                text = "Статистика кредитов:",
                style = MaterialTheme.typography.titleMedium
            )

            LoanBarChart(paidLoans = paidLoans, remainingLoans = remainingLoans)

            Divider()

            Text(
                text = "Совет на сегодня:",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalPager(
                count = motivationalQuotes.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // Уменьшил высоту карточек
            ) { page ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp), // Немного уменьшил отступы
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = motivationalQuotes[page],
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun LoanBarChart(
    paidLoans: Int,
    remainingLoans: Int
) {
    val totalLoans = paidLoans + remainingLoans
    val paidRatio = if (totalLoans > 0) paidLoans.toFloat() / totalLoans else 0f
    val remainingRatio = if (totalLoans > 0) remainingLoans.toFloat() / totalLoans else 0f

    val barWidth = 60.dp
    val barSpacing = 40.dp
    val barCornerRadius = 12.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Погашено
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .fillMaxHeight(paidRatio)
                        .clip(RoundedCornerShape(topStart = barCornerRadius, topEnd = barCornerRadius))
                        .background(Color(0xFF81C784)) // мягкий зелёный
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Погашено", color = Color(0xFF388E3C), style = MaterialTheme.typography.bodySmall)
                Text(text = "$paidLoans", style = MaterialTheme.typography.bodyMedium)
            }

            // Осталось
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .fillMaxHeight(remainingRatio)
                        .clip(RoundedCornerShape(topStart = barCornerRadius, topEnd = barCornerRadius))
                        .background(Color(0xFFE57373)) // мягкий красный
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Осталось", color = Color(0xFFD32F2F), style = MaterialTheme.typography.bodySmall)
                Text(text = "$remainingLoans", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

