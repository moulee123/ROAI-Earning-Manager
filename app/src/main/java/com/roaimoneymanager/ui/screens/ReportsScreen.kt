package com.roaimoneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.domain.model.ReportTimeFrame
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.ui.components.CategorySpendingDonutChart
import com.roaimoneymanager.ui.components.IncomeVsExpenseComparisonBarChart
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()
    var selectedTimeframe by remember { mutableStateOf(ReportTimeFrame.MONTHLY) }

    val filteredTransactions = remember(transactions, selectedTimeframe) {
        val (start, end) = when (selectedTimeframe) {
            ReportTimeFrame.DAILY -> DateUtils.getStartOfDay() to DateUtils.getEndOfDay()
            ReportTimeFrame.WEEKLY -> DateUtils.getStartOfWeek() to DateUtils.getEndOfWeek()
            ReportTimeFrame.MONTHLY -> DateUtils.getStartOfMonth() to DateUtils.getEndOfMonth()
            ReportTimeFrame.YEARLY -> DateUtils.getStartOfYear() to DateUtils.getEndOfYear()
        }
        transactions.filter { it.dateMillis in start..end }
    }

    val filteredCompletedTxs = remember(filteredTransactions) {
        filteredTransactions.filter { it.status == TransactionStatus.COMPLETED }
    }

    val filteredIncomePaise = remember(filteredCompletedTxs) {
        filteredCompletedTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
    }

    val filteredExpensePaise = remember(filteredCompletedTxs) {
        filteredCompletedTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
    }

    val filteredNetCashFlowPaise = remember(filteredIncomePaise, filteredExpensePaise) {
        filteredIncomePaise - filteredExpensePaise
    }

    val filteredSavingsRate = remember(filteredIncomePaise, filteredExpensePaise) {
        if (filteredIncomePaise > 0) {
            (((filteredIncomePaise - filteredExpensePaise).toDouble() / filteredIncomePaise.toDouble()) * 100).toInt().coerceAtLeast(0)
        } else 0
    }

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
                text = "Financial Reports & Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Detailed breakdown of income, expenses & trends.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Timeframe Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReportTimeFrame.entries.forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GreenPrimary else Color.Transparent)
                            .clickable { selectedTimeframe = tf }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Income vs Expenses Chart
            IncomeVsExpenseComparisonBarChart(
                incomePaise = filteredIncomePaise,
                expensePaise = filteredExpensePaise
            )

            Spacer(Modifier.height(20.dp))

            // Spending by Category Donut Chart
            CategorySpendingDonutChart(transactions = filteredTransactions)

            Spacer(Modifier.height(20.dp))

            // Net Cash Flow & Savings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Net Cash Flow & Savings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Net Cash Flow", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val color = if (filteredNetCashFlowPaise >= 0) IncomeGreen else ExpenseRed
                            Text(
                                text = MoneyUtils.formatPaiseToRupees(filteredNetCashFlowPaise),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Savings Rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$filteredSavingsRate%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}
