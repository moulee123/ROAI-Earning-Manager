package com.roaimoneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.ui.components.*
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel
import com.roaimoneymanager.util.MoneyUtils

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToWork: () -> Unit,
    onNavigateToSavings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.financialSummary.collectAsState()
    val openingBalances by viewModel.openingBalances.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var selectedTxType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showOpeningDialog by remember { mutableStateOf(false) }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROAI",
                        color = GreenPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Money Manager",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Your money, under control.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = { showOpeningDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Opening Balances",
                        tint = GreenPrimary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Main Balance Card
            MainBalanceCard(
                summary = summary,
                openingBalances = openingBalances,
                onSetupBalancesClick = { showOpeningDialog = true }
            )

            Spacer(Modifier.height(18.dp))

            // Income / Expense Summary Metric Row
            IncomeExpenseSummaryRow(summary = summary)

            Spacer(Modifier.height(22.dp))

            // Quick Actions
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    title = "+ Income",
                    bgColor = MaterialTheme.colorScheme.primaryContainer,
                    textColor = GreenPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTxType = TransactionType.INCOME
                        editingTx = null
                        showAddTxDialog = true
                    }
                )

                QuickActionButton(
                    title = "− Expense",
                    bgColor = Color(0xFFFADBD8),
                    textColor = ExpenseRed,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTxType = TransactionType.EXPENSE
                        editingTx = null
                        showAddTxDialog = true
                    }
                )

                QuickActionButton(
                    title = "Work Income",
                    bgColor = Color(0xFFE8F8F5),
                    textColor = Color(0xFF117A65),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToWork
                )
            }

            Spacer(Modifier.height(24.dp))

            // Recent Transactions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    modifier = Modifier.clickable(onClick = onNavigateToTransactions)
                )
            }

            Spacer(Modifier.height(12.dp))

            val recentList = transactions.take(5)
            if (recentList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet.\nAdd your first income or expense above!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentList.forEach { tx ->
                        val onClickLambda = remember(tx.id) { {
                            editingTx = tx
                            showAddTxDialog = true
                        } }
                        val onDeleteLambda = remember(tx.id) { {
                            viewModel.deleteTransaction(tx)
                        } }
                        TransactionItem(
                            transaction = tx,
                            onClick = onClickLambda,
                            onDelete = onDeleteLambda
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Savings Goals Progress Preview
            if (savingsGoals.isNotEmpty()) {
                Text(
                    text = "Savings Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSavings),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val topGoal = savingsGoals.first()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(topGoal.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${MoneyUtils.formatPaiseToRupees(topGoal.currentSavedPaise)} / ${MoneyUtils.formatPaiseToRupees(topGoal.targetAmountPaise)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GreenPrimary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { topGoal.progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = GreenPrimary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Monthly Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MONTHLY SAVINGS OVERVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Monthly Inflows", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyUtils.formatPaiseToRupees(summary.monthlyIncomePaise), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }
                        Column {
                            Text("Monthly Outflows", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyUtils.formatPaiseToRupees(summary.monthlyExpensesPaise), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                        Column {
                            Text("Net Savings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val savingsColor = if (summary.monthlyNetSavingsPaise >= 0) IncomeGreen else ExpenseRed
                            Text(MoneyUtils.formatPaiseToRupees(summary.monthlyNetSavingsPaise), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = savingsColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    // Modal Dialogs
    if (showAddTxDialog) {
        AddEditTransactionDialog(
            transaction = editingTx,
            categories = categories,
            onDismiss = { showAddTxDialog = false },
            onSave = { id, amount, title, category, type, method, from, to, date, status, notes ->
                viewModel.saveTransaction(
                    id = id,
                    amountInput = amount,
                    title = title,
                    category = category,
                    type = type,
                    paymentMethod = method,
                    fromAccount = from,
                    toAccount = to,
                    dateMillis = date,
                    status = status,
                    notes = notes,
                    onSuccess = { showAddTxDialog = false }
                )
            }
        )
    }

    if (showOpeningDialog) {
        SetupOpeningBalancesDialog(
            currentBalances = openingBalances,
            onDismiss = { showOpeningDialog = false },
            onSave = { c, b, u ->
                viewModel.saveOpeningBalances(c, b, u)
                showOpeningDialog = false
            }
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
    }
}
