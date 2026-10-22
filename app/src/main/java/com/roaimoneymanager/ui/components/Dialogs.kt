package com.roaimoneymanager.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roaimoneymanager.data.local.entity.*
import com.roaimoneymanager.data.preferences.OpeningBalances
import com.roaimoneymanager.domain.model.*
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity? = null,
    categories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        amount: String,
        title: String,
        category: String,
        type: TransactionType,
        paymentMethod: PaymentMethod,
        fromAccount: String?,
        toAccount: String?,
        dateMillis: Long,
        status: TransactionStatus,
        notes: String
    ) -> Unit
) {
    var type by remember { mutableStateOf(transaction?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(transaction?.let { MoneyUtils.formatPaiseToRupees(it.amountPaise, showSymbol = false) } ?: "") }
    var title by remember { mutableStateOf(transaction?.title ?: "") }
    var category by remember { mutableStateOf(transaction?.category ?: "") }

    var selectedDateMillis by remember { mutableStateOf(transaction?.dateMillis ?: System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var paymentMethod by remember { mutableStateOf(transaction?.paymentMethod ?: PaymentMethod.UPI) }
    var status by remember { mutableStateOf(transaction?.status ?: TransactionStatus.COMPLETED) }
    var notes by remember { mutableStateOf(transaction?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (transaction == null) "Add Transaction" else "Edit Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Type Toggle (Income / Expense / Transfer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    TransactionType.entries.forEach { t ->
                        val isSelected = type == t
                        val btnBg = if (isSelected) {
                            when (t) {
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.TRANSFER -> TransferBlue
                            }
                        } else Color.Transparent

                        val btnText = when (t) {
                            TransactionType.INCOME -> "+ Income"
                            TransactionType.EXPENSE -> "− Expense"
                            TransactionType.TRANSFER -> "⇄ Transfer"
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(btnBg)
                                .clickable {
                                    type = t
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btnText,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) }
                )

                Spacer(Modifier.height(12.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Description") },
                    placeholder = { Text(if (type == TransactionType.INCOME) "e.g., Monthly Salary" else "e.g., Grocery Shopping") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Category Field
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g. Food, Bills, Petrol, Shopping, College") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Transaction Date Selection Field
                OutlinedTextField(
                    value = DateUtils.formatDate(selectedDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Transaction Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = GreenPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Payment Method (UPI Only)
                Text("Payment Method", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("UPI") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = GreenLight,
                        labelColor = GreenPrimary,
                        selectedContainerColor = GreenLight,
                        selectedLabelColor = GreenPrimary
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Status (Completed / Pending)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Payment Completed", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = status == TransactionStatus.COMPLETED,
                        onCheckedChange = { isChecked ->
                            status = if (isChecked) TransactionStatus.COMPLETED else TransactionStatus.PENDING
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = ExpenseRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Title cannot be empty"
                            return@Button
                        }
                        if (!MoneyUtils.isValidAmount(amount)) {
                            errorMessage = "Please enter a valid amount > ₹0"
                            return@Button
                        }

                        val finalCategory = if (category.isBlank()) "General" else category.trim()

                        onSave(
                            transaction?.id ?: 0L,
                            amount,
                            title,
                            finalCategory,
                            type,
                            paymentMethod,
                            null,
                            null,
                            selectedDateMillis,
                            status,
                            notes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(if (transaction == null) "Save Transaction" else "Update Transaction", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            selectedDateMillis = selected
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = GreenPrimary,
                    todayDateBorderColor = GreenPrimary,
                    todayContentColor = GreenPrimary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWorkShiftDialog(
    shift: WorkShiftEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        platform: String,
        gross: String,
        incentives: String,
        hours: String,
        expenses: String,
        dateMillis: Long,
        notes: String,
        autoAddIncome: Boolean
    ) -> Unit
) {
    var platform by remember { mutableStateOf(shift?.platform ?: "") }
    var gross by remember { mutableStateOf(shift?.let { MoneyUtils.formatPaiseToRupees(it.grossEarningsPaise, showSymbol = false) } ?: "") }
    var incentives by remember { mutableStateOf(shift?.let { MoneyUtils.formatPaiseToRupees(it.incentivesPaise, showSymbol = false) } ?: "") }
    var hours by remember { mutableStateOf(shift?.let { if (it.hoursWorked > 0) it.hoursWorked.toString() else "" } ?: "") }
    var expenses by remember { mutableStateOf(shift?.let { MoneyUtils.formatPaiseToRupees(it.expensesPaise, showSymbol = false) } ?: "") }

    var selectedDateMillis by remember { mutableStateOf(shift?.dateMillis ?: System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var notes by remember { mutableStateOf(shift?.notes ?: "") }
    var autoAddIncome by remember { mutableStateOf(shift == null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (shift == null) "Add Work Shift / Income" else "Edit Work Shift",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("Platform / Source") },
                    placeholder = { Text("e.g. Zepto, Swiggy, Instamart, Freelance") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = DateUtils.formatDate(selectedDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Work Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = GreenPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = gross,
                    onValueChange = { gross = it },
                    label = { Text("Gross Earnings (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = incentives,
                    onValueChange = { incentives = it },
                    label = { Text("Incentives / Bonus (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Hours Worked") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = expenses,
                    onValueChange = { expenses = it },
                    label = { Text("Petrol / Work Expenses (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                if (shift == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoAddIncome, onCheckedChange = { autoAddIncome = it })
                        Text("Auto-add net profit to main income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (platform.isBlank()) {
                            errorMsg = "Please enter a platform or source name"
                            return@Button
                        }
                        if (!MoneyUtils.isValidAmount(gross)) {
                            errorMsg = "Enter valid gross earnings"
                            return@Button
                        }
                        onSave(
                            shift?.id ?: 0L,
                            platform.trim(),
                            gross,
                            incentives,
                            hours,
                            expenses,
                            selectedDateMillis,
                            notes,
                            autoAddIncome
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(if (shift == null) "Save Work Shift" else "Update Work Shift", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            selectedDateMillis = selected
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = GreenPrimary,
                    todayDateBorderColor = GreenPrimary,
                    todayContentColor = GreenPrimary
                )
            )
        }
    }
}

@Composable
fun SetupOpeningBalancesDialog(
    currentBalances: OpeningBalances,
    onDismiss: () -> Unit,
    onSave: (cash: Long, bank: Long, upi: Long) -> Unit
) {
    var cashStr by remember { mutableStateOf(MoneyUtils.formatPaiseToRupees(currentBalances.cashPaise, showSymbol = false)) }
    var bankStr by remember { mutableStateOf(MoneyUtils.formatPaiseToRupees(currentBalances.bankPaise, showSymbol = false)) }
    var upiStr by remember { mutableStateOf(MoneyUtils.formatPaiseToRupees(currentBalances.upiPaise, showSymbol = false)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Set Opening Balances", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("Enter existing cash and bank balances to calculate accurate total available balance.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = cashStr,
                    onValueChange = { cashStr = it },
                    label = { Text("Cash Wallet (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bankStr,
                    onValueChange = { bankStr = it },
                    label = { Text("Bank Account (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = upiStr,
                    onValueChange = { upiStr = it },
                    label = { Text("UPI Wallet (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val c = MoneyUtils.parseRupeesToPaise(cashStr)
                        val b = MoneyUtils.parseRupeesToPaise(bankStr)
                        val u = MoneyUtils.parseRupeesToPaise(upiStr)
                        onSave(c, b, u)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Opening Balances", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddEditSavingsGoalDialog(
    goal: SavingsGoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, targetAmount: String, currentSaved: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var targetAmount by remember { mutableStateOf(goal?.let { MoneyUtils.formatPaiseToRupees(it.targetAmountPaise, showSymbol = false) } ?: "") }
    var currentSaved by remember { mutableStateOf(goal?.let { MoneyUtils.formatPaiseToRupees(it.currentSavedPaise, showSymbol = false) } ?: "0") }
    var notes by remember { mutableStateOf(goal?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(if (goal == null) "New Savings Goal" else "Edit Savings Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Name") }, placeholder = { Text("e.g. New Laptop, Emergency Fund") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = targetAmount, onValueChange = { targetAmount = it }, label = { Text("Target Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = currentSaved, onValueChange = { currentSaved = it }, label = { Text("Already Saved (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                // Live Progress Preview Feedback
                val targetPaise = MoneyUtils.parseRupeesToPaise(targetAmount)
                val savedPaise = MoneyUtils.parseRupeesToPaise(currentSaved)
                if (targetPaise > 0L) {
                    Spacer(Modifier.height(8.dp))
                    val remainingPaise = (targetPaise - savedPaise).coerceAtLeast(0L)
                    val percent = ((savedPaise.toFloat() / targetPaise.toFloat()) * 100f).coerceIn(0f, 100f).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progress: $percent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        Text("Remaining: ${MoneyUtils.formatPaiseToRupees(remainingPaise)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMsg = "Goal name cannot be empty"
                            return@Button
                        }
                        if (!MoneyUtils.isValidAmount(targetAmount)) {
                            errorMsg = "Target amount must be > ₹0"
                            return@Button
                        }
                        onSave(goal?.id ?: 0L, title, targetAmount, currentSaved, notes)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Goal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DepositWithdrawGoalDialog(
    goal: SavingsGoalEntity,
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amountStr: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(if (isDeposit) "Add Money to ${goal.title}" else "Withdraw from ${goal.title}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (!MoneyUtils.isValidAmount(amount)) {
                            errorMsg = "Enter valid amount > ₹0"
                            return@Button
                        }
                        onConfirm(amount)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDeposit) GreenPrimary else ExpenseRed)
                ) {
                    Text(if (isDeposit) "Confirm Deposit" else "Confirm Withdrawal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddEditDebtDialog(
    debt: DebtEntity? = null,
    onDismiss: () -> Unit,
    onSave: (id: Long, personName: String, amount: String, type: DebtType, notes: String) -> Unit
) {
    var personName by remember { mutableStateOf(debt?.personName ?: "") }
    var amount by remember { mutableStateOf(debt?.let { MoneyUtils.formatPaiseToRupees(it.amountPaise, showSymbol = false) } ?: "") }
    var type by remember { mutableStateOf(debt?.type ?: DebtType.LENT) }
    var notes by remember { mutableStateOf(debt?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(if (debt == null) "Track Money Owed" else "Edit Money Record", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == DebtType.LENT,
                        onClick = { type = DebtType.LENT },
                        label = { Text("I Lent (They owe me)") },
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    FilterChip(
                        selected = type == DebtType.BORROWED,
                        onClick = { type = DebtType.BORROWED },
                        label = { Text("I Borrowed (I owe)") },
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = personName, onValueChange = { personName = it }, label = { Text("Person Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (personName.isBlank()) {
                            errorMsg = "Person name cannot be empty"
                            return@Button
                        }
                        if (!MoneyUtils.isValidAmount(amount)) {
                            errorMsg = "Enter valid amount > ₹0"
                            return@Button
                        }
                        onSave(debt?.id ?: 0L, personName, amount, type, notes)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DebtRepaymentDialog(
    debt: DebtEntity,
    onDismiss: () -> Unit,
    onSaveRepayment: (amountStr: String, method: PaymentMethod) -> Unit
) {
    var amount by remember { mutableStateOf(MoneyUtils.formatPaiseToRupees(debt.remainingPaise, showSymbol = false)) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Record Repayment for ${debt.personName}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text("Remaining Owed: ${MoneyUtils.formatPaiseToRupees(debt.remainingPaise)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Repayment Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))

                Text("Payment Method", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("UPI") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = GreenLight,
                        labelColor = GreenPrimary,
                        selectedContainerColor = GreenLight,
                        selectedLabelColor = GreenPrimary
                    )
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (!MoneyUtils.isValidAmount(amount)) {
                            errorMsg = "Enter valid amount > ₹0"
                            return@Button
                        }
                        onSaveRepayment(amount, PaymentMethod.UPI)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Record Repayment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Add Custom Category", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("Expense") }, modifier = Modifier.weight(1f).padding(end = 4.dp))
                    FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("Income") }, modifier = Modifier.weight(1f).padding(start = 4.dp))
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, modifier = Modifier.fillMaxWidth())

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMsg = "Name cannot be empty"
                            return@Button
                        }
                        onSave(name, type)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Category", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddEditBudgetDialog(
    budget: BudgetEntity? = null,
    categories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (id: Long, category: String, limitAmount: String, notes: String) -> Unit
) {
    var category by remember { mutableStateOf(budget?.category ?: "") }
    var limitAmount by remember { mutableStateOf(budget?.let { MoneyUtils.formatPaiseToRupees(it.limitAmountPaise, showSymbol = false) } ?: "") }
    var notes by remember { mutableStateOf(budget?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (budget == null) "Set Monthly Budget" else "Edit Monthly Budget",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Food, Petrol, Shopping, College, Bills") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = limitAmount,
                    onValueChange = { limitAmount = it },
                    label = { Text("Monthly Limit Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (category.isBlank()) {
                            errorMsg = "Category name cannot be empty"
                            return@Button
                        }
                        if (!MoneyUtils.isValidAmount(limitAmount)) {
                            errorMsg = "Enter valid limit amount > ₹0"
                            return@Button
                        }
                        onSave(budget?.id ?: 0L, category, limitAmount, notes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(if (budget == null) "Save Budget" else "Update Budget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddEditInvestmentDialog(
    holding: InvestmentHoldingEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        type: String,
        provider: String?,
        accountReference: String?,
        initialAmount: String,
        units: String,
        nav: String,
        currentValue: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(holding?.name ?: "") }
    var type by remember { mutableStateOf(holding?.type ?: "Mutual Funds") }
    var provider by remember { mutableStateOf(holding?.provider ?: "") }
    var accountReference by remember { mutableStateOf(holding?.accountReference ?: "") }
    var initialAmount by remember { mutableStateOf(holding?.let { MoneyUtils.formatPaiseToRupees(it.investedCostPaise, showSymbol = false) } ?: "") }
    var units by remember { mutableStateOf(holding?.let { if (it.units > 0.0) it.units.toString() else "" } ?: "") }
    var nav by remember { mutableStateOf(holding?.let { if (it.nav > 0.0) it.nav.toString() else "" } ?: "") }
    var currentValue by remember { mutableStateOf(holding?.let { MoneyUtils.formatPaiseToRupees(it.currentValuePaise, showSymbol = false) } ?: "") }
    var notes by remember { mutableStateOf(holding?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (holding == null) "Add Investment" else "Edit Investment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Investment Name") },
                    placeholder = { Text("e.g. Nifty 50 Index Fund, Gold ETF, TCS") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Investment Type (Custom)") },
                    placeholder = { Text("e.g. Mutual Funds, Gold, Stocks, Real Estate") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider / Broker (Optional)") },
                    placeholder = { Text("e.g. Groww, Zerodha, HDFC") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialAmount,
                    onValueChange = { initialAmount = it },
                    label = { Text("Invested Amount / Cost Basis (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    label = { Text("Current Market Value (₹) (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it },
                    label = { Text("Units / Quantity (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMsg = "Investment name cannot be empty"
                            return@Button
                        }
                        if (type.isBlank()) {
                            errorMsg = "Investment type cannot be empty"
                            return@Button
                        }
                        if (!MoneyUtils.isValidAmount(initialAmount)) {
                            errorMsg = "Enter valid invested amount > ₹0"
                            return@Button
                        }
                        onSave(
                            holding?.id ?: 0L,
                            name,
                            type,
                            provider,
                            accountReference,
                            initialAmount,
                            units,
                            nav,
                            currentValue,
                            notes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Investment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InvestmentActivityDialog(
    holding: InvestmentHoldingEntity,
    onDismiss: () -> Unit,
    onSubmit: (
        activityType: InvestmentActivityType,
        amount: String,
        units: String,
        nav: String,
        notes: String
    ) -> Unit
) {
    var activityType by remember { mutableStateOf(InvestmentActivityType.VALUATION_UPDATE) }
    var amount by remember { mutableStateOf(MoneyUtils.formatPaiseToRupees(holding.currentValuePaise, showSymbol = false)) }
    var units by remember { mutableStateOf(if (holding.units > 0.0) holding.units.toString() else "") }
    var nav by remember { mutableStateOf(if (holding.nav > 0.0) holding.nav.toString() else "") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Record Activity: ${holding.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InvestmentActivityType.entries.forEach { act ->
                        val selected = activityType == act
                        FilterChip(
                            selected = selected,
                            onClick = {
                                activityType = act
                                when (act) {
                                    InvestmentActivityType.VALUATION_UPDATE -> amount = MoneyUtils.formatPaiseToRupees(holding.currentValuePaise, showSymbol = false)
                                    else -> amount = ""
                                }
                            },
                            label = { Text(act.name.replace("_", " "), fontSize = 11.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                val labelText = when (activityType) {
                    InvestmentActivityType.PURCHASE -> "Purchase Amount (₹)"
                    InvestmentActivityType.REDEMPTION -> "Redemption Amount (₹)"
                    InvestmentActivityType.DIVIDEND -> "Dividend Amount (₹)"
                    InvestmentActivityType.VALUATION_UPDATE -> "New Total Market Value (₹)"
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(labelText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it },
                    label = { Text("Units / Quantity (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (!MoneyUtils.isValidAmount(amount) && activityType != InvestmentActivityType.VALUATION_UPDATE) {
                            errorMsg = "Enter valid amount > ₹0"
                            return@Button
                        }
                        onSubmit(activityType, amount, units, nav, notes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Record Activity", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


