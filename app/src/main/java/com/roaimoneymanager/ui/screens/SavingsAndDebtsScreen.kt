package com.roaimoneymanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.*
import com.roaimoneymanager.domain.model.DebtStatus
import com.roaimoneymanager.domain.model.DebtType
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.ui.components.*
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils
import java.util.Locale

@Composable
fun SavingsAndDebtsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allSavingsGoals.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val holdings by viewModel.allInvestmentHoldings.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 -> Goals, 1 -> Debts, 2 -> Budgets, 3 -> Investments

    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var actionGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var isDepositAction by remember { mutableStateOf(true) }

    var showDebtDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var repaymentDebt by remember { mutableStateOf<DebtEntity?>(null) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

    var showInvestmentDialog by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<InvestmentHoldingEntity?>(null) }
    var activityHolding by remember { mutableStateOf<InvestmentHoldingEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            editingGoal = null
                            showGoalDialog = true
                        }
                        1 -> {
                            editingDebt = null
                            showDebtDialog = true
                        }
                        2 -> {
                            editingBudget = null
                            showBudgetDialog = true
                        }
                        3 -> {
                            editingInvestment = null
                            showInvestmentDialog = true
                        }
                    }
                },
                containerColor = GreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "title") {
                Text(
                    text = "Financial Planning",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item(key = "overview_card") {
                PlanningOverviewCard(
                    goals = goals,
                    debts = debts,
                    budgets = budgets,
                    holdings = holdings,
                    transactions = transactions
                )
            }

            item(key = "tabs") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton(
                            text = "Savings (${goals.size})",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Debts (${debts.size})",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton(
                            text = "Budgets (${budgets.size})",
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Investments (${holdings.size})",
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (goals.isEmpty()) {
                        item(key = "empty_goals") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No savings goals created yet.\nTap + to set your first target!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(goals, key = { "goal_${it.id}" }) { goal ->
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
                1 -> {
                    if (debts.isEmpty()) {
                        item(key = "empty_debts") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No lent or borrowed records.\nTap + to track money lent or borrowed.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(debts, key = { "debt_${it.id}" }) { debt ->
                            DebtItem(
                                debt = debt,
                                onRepay = { repaymentDebt = debt },
                                onDelete = { viewModel.deleteDebt(debt) }
                            )
                        }
                    }
                }
                2 -> {
                    if (budgets.isEmpty()) {
                        item(key = "empty_budgets") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No monthly spending limits set.\nTap + to set a category budget!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(budgets, key = { "budget_${it.id}" }) { budget ->
                            BudgetItem(
                                budget = budget,
                                transactions = transactions,
                                onEdit = {
                                    editingBudget = budget
                                    showBudgetDialog = true
                                },
                                onDelete = { viewModel.deleteBudget(budget) }
                            )
                        }
                    }
                }
                3 -> {
                    if (holdings.isEmpty()) {
                        item(key = "empty_holdings") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "No investments tracked yet.\nTap + to add manually.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { showInvestmentDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Manual Investment", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        item(key = "investments_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Your Holdings",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Button(
                                    onClick = { showInvestmentDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Add Manual",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        items(holdings, key = { "holding_${it.id}" }) { holding ->
                            InvestmentHoldingItem(
                                holding = holding,
                                onActivity = { activityHolding = holding },
                                onDelete = { viewModel.deleteInvestmentHolding(holding) }
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

    if (showBudgetDialog) {
        AddEditBudgetDialog(
            budget = editingBudget,
            categories = categories,
            onDismiss = { showBudgetDialog = false },
            onSave = { id, category, limitStr, notes ->
                viewModel.saveBudget(id, category, limitStr, notes) {
                    showBudgetDialog = false
                }
            }
        )
    }

    if (showInvestmentDialog) {
        AddEditInvestmentDialog(
            holding = editingInvestment,
            onDismiss = { showInvestmentDialog = false },
            onSave = { id, name, type, provider, accountRef, initialAmount, units, nav, currentValue, notes ->
                viewModel.saveInvestmentHolding(
                    id = id,
                    name = name,
                    type = type,
                    provider = provider,
                    accountReference = accountRef,
                    initialAmountInput = initialAmount,
                    unitsInput = units,
                    navInput = nav,
                    currentValueInput = currentValue,
                    notes = notes,
                    onSuccess = { showInvestmentDialog = false }
                )
            }
        )
    }

    if (activityHolding != null) {
        InvestmentActivityDialog(
            holding = activityHolding!!,
            onDismiss = { activityHolding = null },
            onSubmit = { activityType, amount, units, nav, notes ->
                viewModel.recordInvestmentActivity(
                    holding = activityHolding!!,
                    activityType = activityType,
                    amountInput = amount,
                    unitsInput = units,
                    navInput = nav,
                    notes = notes,
                    onSuccess = { activityHolding = null }
                )
            }
        )
    }
}

@Composable
fun PlanningOverviewCard(
    goals: List<SavingsGoalEntity>,
    debts: List<DebtEntity>,
    budgets: List<BudgetEntity>,
    holdings: List<InvestmentHoldingEntity>,
    transactions: List<TransactionEntity>
) {
    val totalTarget = goals.sumOf { it.targetAmountPaise }
    val totalSaved = goals.sumOf { it.currentSavedPaise }
    val savingsRatio = if (totalTarget > 0) (totalSaved.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f) else 0f

    val totalLent = debts.filter { it.type == DebtType.LENT && it.status == DebtStatus.PENDING }.sumOf { it.remainingPaise }
    val totalBorrowed = debts.filter { it.type == DebtType.BORROWED && it.status == DebtStatus.PENDING }.sumOf { it.remainingPaise }

    val monthStart = remember { DateUtils.getStartOfMonth() }
    val monthEnd = remember { DateUtils.getEndOfMonth() }

    val totalBudgetLimit = budgets.sumOf { it.limitAmountPaise }
    val totalBudgetSpent = budgets.sumOf { b ->
        transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
            tx.status == TransactionStatus.COMPLETED &&
            tx.dateMillis in monthStart..monthEnd &&
            tx.category.equals(b.category, ignoreCase = true)
        }.sumOf { it.amountPaise }
    }

    val totalInvested = holdings.sumOf { it.investedCostPaise }
    val currentPortfolioValue = holdings.sumOf { it.currentValuePaise }
    val unrealizedProfit = currentPortfolioValue - totalInvested

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLANNING DASHBOARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Investment Portfolio Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Portfolio Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyUtils.formatPaiseToRupees(currentPortfolioValue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Unrealized P&L", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val pnlColor = if (unrealizedProfit >= 0) IncomeGreen else ExpenseRed
                    Text("${if (unrealizedProfit >= 0) "+" else ""}${MoneyUtils.formatPaiseToRupees(unrealizedProfit)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pnlColor)
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(10.dp))

            // Savings Progress Row
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Savings Progress", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${MoneyUtils.formatPaiseToRupees(totalSaved)} / ${MoneyUtils.formatPaiseToRupees(totalTarget)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { savingsRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GreenPrimary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(10.dp))

            // Metrics Grid: Debts & Budgets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lent: ${MoneyUtils.formatPaiseToRupees(totalLent)}", fontSize = 11.sp, color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                    Text("Borrowed: ${MoneyUtils.formatPaiseToRupees(totalBorrowed)}", fontSize = 11.sp, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Budget Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${MoneyUtils.formatPaiseToRupees(totalBudgetSpent)} / ${MoneyUtils.formatPaiseToRupees(totalBudgetLimit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (totalBudgetSpent > totalBudgetLimit) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun BudgetItem(
    budget: BudgetEntity,
    transactions: List<TransactionEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val monthStart = remember { DateUtils.getStartOfMonth() }
    val monthEnd = remember { DateUtils.getEndOfMonth() }

    val spentPaise = remember(transactions, budget.category) {
        transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
            tx.status == TransactionStatus.COMPLETED &&
            tx.dateMillis in monthStart..monthEnd &&
            tx.category.equals(budget.category, ignoreCase = true)
        }.sumOf { it.amountPaise }
    }

    val remainingPaise = (budget.limitAmountPaise - spentPaise).coerceAtLeast(0L)
    val progressRatio = if (budget.limitAmountPaise > 0) {
        (spentPaise.toFloat() / budget.limitAmountPaise.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isExceeded = spentPaise > budget.limitAmountPaise
    val isWarning = progressRatio >= 0.8f && !isExceeded

    val barColor = when {
        isExceeded -> ExpenseRed
        isWarning -> WarningYellow
        else -> GreenPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = budget.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isExceeded) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ExpenseRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EXCEEDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        } else if (isWarning) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarningYellow.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("NEAR LIMIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningYellow)
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Spent: ${MoneyUtils.formatPaiseToRupees(spentPaise)} / Limit: ${MoneyUtils.formatPaiseToRupees(budget.limitAmountPaise)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining: ${MoneyUtils.formatPaiseToRupees(remainingPaise)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isExceeded) ExpenseRed else IncomeGreen
                )

                Text(
                    text = "${(progressRatio * 100).toInt()}% Used",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = barColor
                )
            }
        }
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
                    fontSize = 12.sp,
                    color = IncomeGreen
                )
                Text(
                    "Remaining: ${MoneyUtils.formatPaiseToRupees((goal.targetAmountPaise - goal.currentSavedPaise).coerceAtLeast(0L))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onWithdraw,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("Withdraw", fontSize = 12.sp, color = ExpenseRed, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = onDeposit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("+ Add savings", fontSize = 12.sp, color = Color.White, maxLines = 1, softWrap = false)
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

@Composable
fun InvestmentHoldingItem(
    holding: InvestmentHoldingEntity,
    onActivity: () -> Unit,
    onDelete: () -> Unit
) {
    var showReconciliation by remember { mutableStateOf(false) }

    val profitLoss = holding.unrealizedProfitLossPaise
    val returnPct = holding.unrealizedReturnPercentage
    val isProfit = profitLoss >= 0L
    val profitColor = if (isProfit) IncomeGreen else ExpenseRed
    val sign = if (isProfit) "+" else ""

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
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GreenPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(holding.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(holding.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!holding.provider.isNullOrBlank()) {
                        Text(holding.provider, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        MoneyUtils.formatPaiseToRupees(holding.currentValuePaise),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$sign${MoneyUtils.formatPaiseToRupees(profitLoss)} (${String.format(Locale.getDefault(), "%.2f", returnPct)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(10.dp))

            if (showReconciliation) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("Holdings Reconciliation (Groww Sync)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Scheme Code / ISIN:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(holding.schemeCode ?: "N/A", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Units Held:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (holding.units > 0.0) holding.units.toString() else "N/A", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Latest NAV:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (holding.nav > 0.0) "₹${holding.nav}" else "N/A", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Invested Cost Basis:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(MoneyUtils.formatPaiseToRupees(holding.investedCostPaise), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NAV Date:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DateUtils.formatDate(holding.lastValuationDateMillis), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Cost: ${MoneyUtils.formatPaiseToRupees(holding.investedCostPaise)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Valued: ${DateUtils.formatDate(holding.lastValuationDateMillis)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.clickable { showReconciliation = !showReconciliation }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    Button(
                        onClick = onActivity,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Activity", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) GreenPrimary else Color.Transparent
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
