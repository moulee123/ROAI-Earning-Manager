package com.roaimoneymanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.util.MoneyUtils

@Composable
fun CategorySpendingDonutChart(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val expenseTxs = transactions.filter { it.type == TransactionType.EXPENSE }
    val categoryTotals = expenseTxs.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amountPaise } }
        .filterValues { it > 0 }
        .entries
        .sortedByDescending { it.value }

    val totalExpensePaise = categoryTotals.sumOf { it.value }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Spending by Category",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            if (totalExpensePaise == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded for the selected period.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                val chartColors = listOf(
                    Color(0xFF2ECC71), Color(0xFFFF7043), Color(0xFF42A5F5),
                    Color(0xFFAB47BC), Color(0xFFEC407A), Color(0xFFFFCA28),
                    Color(0xFF78909C), Color(0xFF8D6E63), Color(0xFF26A69A), Color(0xFF5C6BC0)
                )

                // Donut Chart Centered at Top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 32f

                            categoryTotals.forEachIndexed { index, entry ->
                                val sweepAngle = (entry.value.toFloat() / totalExpensePaise.toFloat()) * 360f
                                val color = chartColors[index % chartColors.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                MoneyUtils.formatPaiseToRupees(totalExpensePaise),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Category Legend Stacked Below Chart
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryTotals.forEachIndexed { index, entry ->
                        val color = chartColors[index % chartColors.size]
                        val pctDouble = (entry.value.toDouble() / totalExpensePaise.toDouble()) * 100
                        val percentageStr = if (pctDouble in 0.0001..0.9999) "<1" else pctDouble.toInt().toString()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color Indicator
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )

                            Spacer(Modifier.width(10.dp))

                            // Category Name (Ellipsized if long)
                            Text(
                                text = entry.key,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(Modifier.width(8.dp))

                            // Amount
                            Text(
                                text = MoneyUtils.formatPaiseToRupees(entry.value),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )

                            Spacer(Modifier.width(8.dp))

                            // Percentage Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$percentageStr%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseComparisonBarChart(
    incomePaise: Long,
    expensePaise: Long,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(incomePaise, expensePaise, 1L).toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Income vs Expenses",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Income Bar
                BarItem(
                    label = "Total Income",
                    amountStr = MoneyUtils.formatPaiseToRupees(incomePaise),
                    progress = (incomePaise.toFloat() / maxVal).coerceIn(0f, 1f),
                    barColor = IncomeGreen
                )

                // Expense Bar
                BarItem(
                    label = "Total Expense",
                    amountStr = MoneyUtils.formatPaiseToRupees(expensePaise),
                    progress = (expensePaise.toFloat() / maxVal).coerceIn(0f, 1f),
                    barColor = ExpenseRed
                )
            }
        }
    }
}

@Composable
fun BarItem(label: String, amountStr: String, progress: Float, barColor: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = amountStr,
                fontSize = 13.sp,
                color = barColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f)
        )
    }
}
