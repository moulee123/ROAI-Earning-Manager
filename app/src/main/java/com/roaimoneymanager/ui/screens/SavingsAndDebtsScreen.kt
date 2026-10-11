package com.roaimoneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.DebtEntity
import com.roaimoneymanager.data.local.entity.SavingsGoalEntity
import com.roaimoneymanager.domain.model.DebtStatus
import com.roaimoneymanager.domain.model.DebtType
import com.roaimoneymanager.ui.components.*
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel
import com.roaimoneymanager.util.MoneyUtils

@Composable
fun SavingsAndDebtsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allSavingsGoals.collectAsState()
    val debts by viewModel.allDebts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 -> Goals, 1 -> Debts

    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var actionGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var isDepositAction by remember { mutableStateOf(true) }

    var showDebtDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var repaymentDebt by remember { mutableStateOf<DebtEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingGoal = null
                        showGoalDialog = true
                    } else {
                        editingDebt = null
                        showDebtDialog = true
                    }
                },
                containerColor = GreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Savings & Debts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(14.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GreenPrimary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Savings Goals (${goals.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Money Owed (${debts.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Goals List
                if (goals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No savings goals created yet.\nTap + to set your first target!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(goals, key = { it.id }) { goal ->
                            SavingsGoalItem(
                                goal = goal,
                                onDeposit = {
                                    actionGoal = goal
                                    isDepositAction = true
                                },
                                onWithdraw = {
                                    actionGoal = goal
                                    isDepositAction = false
                                },
                                onDelete = { viewModel.deleteSavingsGoal(goal) }
                            )
                        }
                    }
                }
            } else {
                // Debts List
                if (debts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No lent or borrowed records.\nTap + to track money lent to others or borrowed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(debts, key = { it.id }) { debt ->
                            DebtItem(
                                debt = debt,
                                onRepay = { repaymentDebt = debt },
                                onDelete = { viewModel.deleteDebt(debt) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        AddEditSavingsGoalDialog(
            goal = editingGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { id, title, target, current, notes ->
                viewModel.saveSavingsGoal(id, title, target, current, notes) {
                    showGoalDialog = false
                }
            }
        )
    }

    if (actionGoal != null) {
        DepositWithdrawGoalDialog(
            goal = actionGoal!!,
            isDeposit = isDepositAction,
            onDismiss = { actionGoal = null },
            onConfirm = { amountStr ->
                if (isDepositAction) {
                    viewModel.depositToSavingsGoal(actionGoal!!, amountStr) { actionGoal = null }
                } else {
                    viewModel.depositToSavingsGoal(actionGoal!!, "-$amountStr") { actionGoal = null }
                }
            }
        )
    }

    if (showDebtDialog) {
        AddEditDebtDialog(
            debt = editingDebt,
            onDismiss = { showDebtDialog = false },
            onSave = { id, name, amount, type, notes ->
                viewModel.saveDebt(id, name, amount, type, notes) {
                    showDebtDialog = false
                }
            }
        )
    }

    if (repaymentDebt != null) {
        DebtRepaymentDialog(
            debt = repaymentDebt!!,
            onDismiss = { repaymentDebt = null },
            onSaveRepayment = { amountStr, method ->
                viewModel.addDebtRepayment(repaymentDebt!!, amountStr, method) {
                    repaymentDebt = null
                }
            }
        )
    }
}

@Composable
fun SavingsGoalItem(
    goal: SavingsGoalEntity,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = "Goal", tint = GreenPrimary)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Target: ${MoneyUtils.formatPaiseToRupees(goal.targetAmountPaise)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { goal.progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = GreenPrimary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Saved: ${MoneyUtils.formatPaiseToRupees(goal.currentSavedPaise)} (${(goal.progressRatio * 100).toInt()}%)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = IncomeGreen
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onWithdraw,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Withdraw", fontSize = 11.sp, color = ExpenseRed)
                    }

                    Button(
                        onClick = onDeposit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("+ Deposit", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DebtItem(
    debt: DebtEntity,
    onRepay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val badgeText = if (debt.type == DebtType.LENT) "LENT TO" else "BORROWED FROM"
                    val badgeBg = if (debt.type == DebtType.LENT) GreenLight else Color(0xFFFADBD8)
                    val badgeColor = if (debt.type == DebtType.LENT) IncomeGreen else ExpenseRed

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(debt.personName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        MoneyUtils.formatPaiseToRupees(debt.amountPaise),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (debt.type == DebtType.LENT) IncomeGreen else ExpenseRed
                    )
                    if (debt.status == DebtStatus.SETTLED) {
                        Text("SETTLED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    } else {
                        Text("Remaining: ${MoneyUtils.formatPaiseToRupees(debt.remainingPaise)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }

                if (debt.status == DebtStatus.PENDING) {
                    Button(
                        onClick = onRepay,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("Record Repayment", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
