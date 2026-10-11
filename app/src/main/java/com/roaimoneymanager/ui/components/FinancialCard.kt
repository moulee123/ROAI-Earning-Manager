package com.roaimoneymanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.preferences.OpeningBalances
import com.roaimoneymanager.domain.calculator.FinancialSummary
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.util.MoneyUtils

@Composable
fun MainBalanceCard(
    summary: FinancialSummary,
    openingBalances: OpeningBalances,
    onSetupBalancesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedOpening by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GreenPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL AVAILABLE BALANCE",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { expandedOpening = !expandedOpening }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Opening Details",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (expandedOpening) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = MoneyUtils.formatPaiseToRupees(summary.availableBalancePaise),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Includes Opening Balances + Actual Cash Flow",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )

            // Collapsible Opening Balance Breakdown
            AnimatedVisibility(visible = expandedOpening) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Opening Balances Breakdown",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OpeningRow(icon = Icons.Default.AccountBalanceWallet, label = "Cash Wallet", amount = openingBalances.cashPaise)
                        OpeningRow(icon = Icons.Default.AccountBalance, label = "Bank Account", amount = openingBalances.bankPaise)
                        OpeningRow(icon = Icons.Default.QrCodeScanner, label = "UPI Wallet", amount = openingBalances.upiPaise)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onSetupBalancesClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Edit Opening Balances", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OpeningRow(icon: ImageVector, label: String, amount: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = Color.White.copy(0.8f), fontSize = 10.sp)
        Text(text = MoneyUtils.formatPaiseToRupees(amount), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun IncomeExpenseSummaryRow(
    summary: FinancialSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryMetricCard(
            title = "Total Income",
            amount = MoneyUtils.formatPaiseToRupees(summary.totalIncomePaise),
            subtitle = "Today: ${MoneyUtils.formatPaiseToRupees(summary.todayIncomePaise)}",
            amountColor = IncomeGreen,
            modifier = Modifier.weight(1f)
        )

        SummaryMetricCard(
            title = "Total Expenses",
            amount = MoneyUtils.formatPaiseToRupees(summary.totalExpensesPaise),
            subtitle = "Today: ${MoneyUtils.formatPaiseToRupees(summary.todayExpensesPaise)}",
            amountColor = ExpenseRed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryMetricCard(
    title: String,
    amount: String,
    subtitle: String,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(amount, color = amountColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
