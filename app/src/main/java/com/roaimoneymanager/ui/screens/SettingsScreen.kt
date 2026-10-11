package com.roaimoneymanager.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.ui.components.AddCategoryDialog
import com.roaimoneymanager.ui.components.SetupOpeningBalancesDialog
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val openingBalances by viewModel.openingBalances.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var showOpeningDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Settings & Data",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage accounts, categories & offline backups.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // Section: Accounts & Preferences
            Text("ACCOUNT SETUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            SettingsTile(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Setup Opening Balances",
                subtitle = "Configure initial cash, bank and UPI wallet amounts",
                onClick = { showOpeningDialog = true }
            )

            Spacer(Modifier.height(12.dp))

            SettingsTile(
                icon = Icons.Default.Category,
                title = "Custom Categories",
                subtitle = "Manage expense and income categories (${categories.size} total)",
                onClick = { showCategoryDialog = true }
            )

            Spacer(Modifier.height(24.dp))

            // Section: Data & Backup
            Text("DATA EXPORT & BACKUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            SettingsTile(
                icon = Icons.Default.FileDownload,
                title = "Export Transactions to CSV",
                subtitle = "Share or save CSV spreadsheet file",
                onClick = {
                    val csvData = viewModel.getCsvData()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csvData)
                        type = "text/csv"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export Transactions CSV"))
                }
            )

            Spacer(Modifier.height(12.dp))

            SettingsTile(
                icon = Icons.Default.Backup,
                title = "Export JSON Backup",
                subtitle = "Create full offline backup of all app records",
                onClick = {
                    scope.launch {
                        val json = viewModel.getBackupJson()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, json)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export ROAI Backup"))
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Privacy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Privacy",
                        tint = GreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("100% Offline & Private", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GreenDark)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "ROAI Money Manager stores all your financial data strictly on your device. Bank passwords and UPI PINs are never requested or stored.",
                            fontSize = 12.sp,
                            color = GreenDark.copy(alpha = 0.85f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showOpeningDialog) {
        SetupOpeningBalancesDialog(
            currentBalances = openingBalances,
            onDismiss = { showOpeningDialog = false },
            onSave = { c, b, u ->
                viewModel.saveOpeningBalances(c, b, u)
                showOpeningDialog = false
                Toast.makeText(context, "Opening balances updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = { name, type ->
                viewModel.saveCategory(name, type) {
                    showCategoryDialog = false
                    Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = GreenPrimary)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.LightGray
            )
        }
    }
}
