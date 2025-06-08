package com.kreditnik.app.ui.screens

import androidx.compose.foundation.layout.*
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kreditnik.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val motivationalQuotes = remember {
        context.resources.getStringArray(R.array.motivational_quotes).toList()
    }

    val pagerState = rememberPagerState(initialPage = 0)

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Всего кредитов: 5",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Сумма задолженности: 250 000 ₽",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))
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
                    .height(150.dp)
            ) { page ->
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
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
