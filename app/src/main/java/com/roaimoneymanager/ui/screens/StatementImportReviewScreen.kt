package com.roaimoneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.CategoryEntity
import com.roaimoneymanager.domain.model.*
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.ui.viewmodel.MainViewModel
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementImportReviewScreen(
    viewModel: MainViewModel,
    onFinishImport: () -> Unit,
    onCancelImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val importState by viewModel.statementImportState.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val reviewingState = importState as? StatementImportState.Reviewing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Review Bank Statement", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onCancelImport) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (reviewingState != null) {
                val summary = reviewingState.summary
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { viewModel.confirmAndSaveImportedTransactions() },
                            enabled = summary.totalSelected > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text(
                                text = "Confirm & Save (${summary.totalSelected} Transactions)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (val state = importState) {
                is StatementImportState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No statement selected.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                is StatementImportState.Processing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 4.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Extracting & analyzing locally on your phone...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is StatementImportState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = ExpenseRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Import Notice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onCancelImport,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is StatementImportState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = IncomeGreen,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Import Successful!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${state.importedCount} transactions imported to your local history.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Income Added", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyUtils.formatPaiseToRupees(state.totalCreditPaise), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IncomeGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Expenses Added", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyUtils.formatPaiseToRupees(state.totalDebitPaise), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ExpenseRed)
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = onFinishImport,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is StatementImportState.Reviewing -> {
                    val summary = state.summary
                    val candidates = state.candidates

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Statement Summary Header Card (Compact & Responsive)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenPrimary)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(summary.bankName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(summary.fileName, color = Color.White.copy(0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    val reconText = when (summary.reconciliationStatus) {
                                        ReconciliationStatus.CONSISTENT -> "RECONCILED ✓"
                                        ReconciliationStatus.NEEDS_REVIEW -> "NEEDS REVIEW"
                                        ReconciliationStatus.UNAVAILABLE -> "RAW"
                                    }
                                    val reconBg = when (summary.reconciliationStatus) {
                                        ReconciliationStatus.CONSISTENT -> IncomeGreen
                                        ReconciliationStatus.NEEDS_REVIEW -> WarningYellow
                                        ReconciliationStatus.UNAVAILABLE -> Color.Gray
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(reconBg)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reconText,
                                            color = if (summary.reconciliationStatus == ReconciliationStatus.NEEDS_REVIEW) DarkText else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Selected / Total", color = Color.White.copy(0.7f), fontSize = 10.sp)
                                        Text("${summary.totalSelected} / ${summary.totalCandidates}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("Income", color = Color.White.copy(0.7f), fontSize = 10.sp)
                                        Text(MoneyUtils.formatPaiseToRupees(summary.totalCreditPaise), color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("Expense", color = Color.White.copy(0.7f), fontSize = 10.sp)
                                        Text(MoneyUtils.formatPaiseToRupees(summary.totalDebitPaise), color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Controls Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.selectAllCandidates(true) }) {
                                Text("Select All", fontSize = 12.sp, color = GreenPrimary)
                            }
                            if (summary.likelyDuplicateCount > 0) {
                                TextButton(onClick = { viewModel.deselectLikelyDuplicates() }) {
                                    Text("Skip Duplicates (${summary.likelyDuplicateCount})", fontSize = 12.sp, color = ExpenseRed)
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Candidate List (Independent scrolling, non-clipped)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(candidates, key = { it.id }) { candidate ->
                                CandidateRowItem(
                                    candidate = candidate,
                                    categories = categories,
                                    onToggleSelect = { viewModel.toggleCandidateSelection(candidate.id) },
                                    onUpdateCandidate = { updated -> viewModel.updateCandidate(updated) }
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
fun CandidateRowItem(
    candidate: CandidateTransaction,
    categories: List<CategoryEntity>,
    onToggleSelect: () -> Unit,
    onUpdateCandidate: (CandidateTransaction) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isIncome = candidate.isIncome
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val prefix = if (isIncome) "+" else "−"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = candidate.isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.scale(0.85f)
                )

                Spacer(Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = candidate.description,
                        onValueChange = { onUpdateCandidate(candidate.copy(description = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Display Name", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DateUtils.formatDate(candidate.dateMillis),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = candidate.paymentMethod.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        if (candidate.confidence == ExtractionConfidence.LOW_OCR) {
                            Spacer(Modifier.width(6.dp))
                            Text("OCR Low Conf.", fontSize = 10.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$prefix${MoneyUtils.formatPaiseToRupees(candidate.amountPaise)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = amountColor,
                        maxLines = 1,
                        softWrap = false
                    )
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(if (isExpanded) "Less" else "Details", fontSize = 11.sp)
                    }
                }
            }

            if (isExpanded) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = candidate.notes,
                    onValueChange = { onUpdateCandidate(candidate.copy(notes = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Personal Reason / Notes", fontSize = 10.sp) },
                    textStyle = TextStyle(fontSize = 12.sp)
                )

                Spacer(Modifier.height(6.dp))

                var categoryExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Category: ${candidate.category}", fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    onUpdateCandidate(candidate.copy(category = cat.name))
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Original Narration: ${candidate.originalDescription}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!candidate.refNumber.isNullOrBlank()) {
                    Text(
                        text = "Ref No: ${candidate.refNumber}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (candidate.duplicateConfidence != DuplicateConfidence.NO_DUPLICATE && !candidate.duplicateReason.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                val warnBg = if (candidate.duplicateConfidence == DuplicateConfidence.LIKELY_DUPLICATE) {
                    ExpenseRed.copy(alpha = 0.15f)
                } else {
                    WarningYellow.copy(alpha = 0.15f)
                }
                val warnColor = if (candidate.duplicateConfidence == DuplicateConfidence.LIKELY_DUPLICATE) {
                    ExpenseRed
                } else {
                    WarningYellow
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(warnBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = warnColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = candidate.duplicateReason,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = warnColor
                        )
                    }
                }
            }
        }
    }
}
