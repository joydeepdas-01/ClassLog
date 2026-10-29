package com.classlog.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.classlog.app.ui.home.TodayScreen
import com.classlog.app.ui.home.TodayViewModel
import com.classlog.app.ui.records.RecordsScreen
import com.classlog.app.ui.records.RecordsViewModel
import com.classlog.app.ui.setup.SetupNavHost
import com.classlog.app.ui.setup.SetupViewModel

import com.classlog.app.ui.summary.SummaryScreen
import com.classlog.app.ui.summary.SummaryViewModel

enum class AppTab(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.Today),
    RECORDS("Records", Icons.Default.DateRange),
    SUMMARY("Summary", Icons.Default.BarChart),
    SETUP("Setup", Icons.Default.Settings)
}

@Composable
fun MainAppScaffold(
    todayViewModel: TodayViewModel,
    recordsViewModel: RecordsViewModel,
    summaryViewModel: SummaryViewModel,
    setupViewModel: SetupViewModel
) {
    var selectedTab by remember { mutableStateOf(AppTab.TODAY) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AppTab.TODAY -> {
                    TodayScreen(
                        viewModel = todayViewModel,
                        onNavigateSetup = { selectedTab = AppTab.SETUP }
                    )
                }
                AppTab.RECORDS -> {
                    RecordsScreen(viewModel = recordsViewModel)
                }
                AppTab.SUMMARY -> {
                    SummaryScreen(viewModel = summaryViewModel)
                }
                AppTab.SETUP -> {
                    SetupNavHost(viewModel = setupViewModel)
                }
            }
        }
    }
}
