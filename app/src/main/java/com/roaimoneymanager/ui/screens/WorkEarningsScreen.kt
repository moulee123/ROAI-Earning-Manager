package com.roaimoneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.WorkShiftEntity
import com.roaimoneymanager.ui.components.AddEditWorkShiftDialog
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils

@Composable
fun WorkEarningsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val workShifts by viewModel.allWorkShifts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<WorkShiftEntity?>(null) }

    val totalGrossPaise = remember(workShifts) { workShifts.sumOf { it.grossEarningsPaise } }
    val totalIncentivesPaise = remember(workShifts) { workShifts.sumOf { it.incentivesPaise } }
    val totalExpensesPaise = remember(workShifts) { workShifts.sumOf { it.expensesPaise } }
    val totalNetProfitPaise = remember(workShifts) { workShifts.sumOf { it.netProfitPaise } }
    val totalHours = remember(workShifts) { workShifts.sumOf { it.hoursWorked } }

    val avgHourlyRatePaise = remember(totalNetProfitPaise, totalHours) {
        if (totalHours > 0) (totalNetProfitPaise.toDouble() / totalHours).toLong() else 0L
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingShift = null
                    showDialog = true
                },
                containerColor = GreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Work Shift")
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
                text = "Work Earnings & Delivery",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track Swiggy, Zomato, Uber & freelance shifts.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Work Summary Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("NET WORK PROFIT", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = MoneyUtils.formatPaiseToRupees(totalNetProfitPaise),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkMetric(
                            label = "Gross + Bonus",
                            value = MoneyUtils.formatPaiseToRupees(totalGrossPaise + totalIncentivesPaise),
                            modifier = Modifier.weight(1f)
                        )
                        WorkMetric(
                            label = "Work Expenses",
                            value = MoneyUtils.formatPaiseToRupees(totalExpensesPaise),
                            modifier = Modifier.weight(1f)
                        )
                        WorkMetric(
                            label = "Avg Net/Hr",
                            value = MoneyUtils.formatPaiseToRupees(avgHourlyRatePaise),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Shift History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))

            if (workShifts.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "No work shifts recorded yet.\nTap + to log your first delivery shift!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(workShifts, key = { it.id }) { shift ->
                        WorkShiftItem(
                            shift = shift,
                            onClick = {
                                editingShift = shift
                                showDialog = true
                            },
                            onDelete = { viewModel.deleteWorkShift(shift) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddEditWorkShiftDialog(
            shift = editingShift,
            onDismiss = { showDialog = false },
            onSave = { id, platform, gross, incentives, hours, expenses, dateMillis, notes, autoAdd ->
                viewModel.saveWorkShift(
                    id = id,
                    platform = platform,
                    grossInput = gross,
                    incentivesInput = incentives,
                    hoursWorkedInput = hours,
                    expensesInput = expenses,
                    dateMillis = dateMillis,
                    notes = notes,
                    autoAddIncomeTransaction = autoAdd,
                    onSuccess = { showDialog = false }
                )
            }
        )
    }
}

@Composable
fun WorkMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.White.copy(0.75f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun WorkShiftItem(
    shift: WorkShiftEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Work Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = "Work",
                    tint = GreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // CENTER: Details (Takes weight(1f))
            Column(modifier = Modifier.weight(1f)) {
                // Platform / Source Name (Ellipsized if long)
                Text(
                    text = shift.platform,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                // Date & Hours Worked
                val hoursText = if (shift.hoursWorked > 0) " • ${shift.hoursWorked} hrs" else ""
                Text(
                    text = "${DateUtils.formatDate(shift.dateMillis)}$hoursText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(Modifier.height(2.dp))

                // Gross & Fuel/Work Expenses Breakdown
                val grossStr = MoneyUtils.formatPaiseToRupees(shift.grossEarningsPaise + shift.incentivesPaise)
                val expenseStr = MoneyUtils.formatPaiseToRupees(shift.expensesPaise)
                Text(
                    text = "Gross: $grossStr | Exp: $expenseStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            // RIGHT: Net Profit Amount & Delete Action
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "+${MoneyUtils.formatPaiseToRupees(shift.netProfitPaise)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = IncomeGreen,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(Modifier.height(2.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
