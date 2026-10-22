package com.roaimoneymanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "History", Icons.Default.ReceiptLong)
    object Work : Screen("work", "Work", Icons.Default.Work)
    object Savings : Screen("savings", "Planning", Icons.Default.Savings)
    object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object StatementImport : Screen("statement_import", "Import", Icons.Default.FileUpload)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Work,
    Screen.Savings,
    Screen.Reports,
    Screen.Settings
)
