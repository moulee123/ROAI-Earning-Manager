package com.roaimoneymanager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.ui.components.AddEditTransactionDialog
import com.roaimoneymanager.ui.components.TransactionItem
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel

@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE", "TRANSFER", "PENDING"
    var showDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val filteredList = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                "INCOME" -> tx.type == TransactionType.INCOME
                "EXPENSE" -> tx.type == TransactionType.EXPENSE
                "TRANSFER" -> tx.type == TransactionType.TRANSFER
                "PENDING" -> tx.status == TransactionStatus.PENDING
                else -> true
            }
            val matchesQuery = tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.notes.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTx = null
                    showDialog = true
                },
                containerColor = GreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                text = "Transactions History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All",
                    "INCOME" to "Incomes",
                    "EXPENSE" to "Expenses",
                    "TRANSFER" to "Transfers",
                    "PENDING" to "Pending"
                )

                filters.forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = GreenPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching transactions found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onClick = {
                                editingTx = tx
                                showDialog = true
                            },
                            onDelete = {
                                viewModel.deleteTransaction(tx)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddEditTransactionDialog(
            transaction = editingTx,
            categories = categories,
            onDismiss = { showDialog = false },
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
                    onSuccess = { showDialog = false }
                )
            }
        )
    }
}
