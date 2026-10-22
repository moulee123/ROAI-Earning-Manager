package com.roaimoneymanager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roaimoneymanager.data.export.BackupRestoreManager
import com.roaimoneymanager.data.export.CsvExporter
import com.roaimoneymanager.data.local.AppDatabase
import com.roaimoneymanager.data.local.entity.*
import com.roaimoneymanager.data.preferences.OpeningBalances
import com.roaimoneymanager.data.preferences.UserPreferencesRepository
import com.roaimoneymanager.data.repository.FinanceRepository
import com.roaimoneymanager.data.statement.BalanceReconciliationService
import com.roaimoneymanager.data.statement.DuplicateDetectionService
import com.roaimoneymanager.data.statement.LocalPdfExtractor
import com.roaimoneymanager.data.statement.UniversalStatementParser
import java.io.File
import com.roaimoneymanager.domain.calculator.FinancialCalculator
import com.roaimoneymanager.domain.calculator.FinancialSummary
import com.roaimoneymanager.domain.model.*
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = FinanceRepository(database)
    val userPreferencesRepository = UserPreferencesRepository(application)

    // Flow states
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkShifts: StateFlow<List<WorkShiftEntity>> = repository.allWorkShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<DebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openingBalances: StateFlow<OpeningBalances> = userPreferencesRepository.openingBalancesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OpeningBalances())

    // Statement Import State
    private val _statementImportState = MutableStateFlow<StatementImportState>(StatementImportState.Idle)
    val statementImportState: StateFlow<StatementImportState> = _statementImportState.asStateFlow()

    // Financial Summary State
    val financialSummary: StateFlow<FinancialSummary> = combine(
        allTransactions,
        openingBalances
    ) { txList, balances ->
        val todayStart = DateUtils.getStartOfDay()
        val todayEnd = DateUtils.getEndOfDay()
        val monthStart = DateUtils.getStartOfMonth()
        val monthEnd = DateUtils.getEndOfMonth()

        FinancialCalculator.calculateSummary(
            transactions = txList,
            openingBalances = balances,
            todayStartMillis = todayStart,
            todayEndMillis = todayEnd,
            monthStartMillis = monthStart,
            monthEndMillis = monthEnd
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    // UI Toast or Error Messages
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Setup opening balances
    fun saveOpeningBalances(cashPaise: Long, bankPaise: Long, upiPaise: Long) {
        viewModelScope.launch {
            userPreferencesRepository.updateOpeningBalances(cashPaise, bankPaise, upiPaise)
            _uiEvent.emit("Opening balances updated successfully")
        }
    }

    // Add / Update Transaction with Validation
    fun saveTransaction(
        id: Long = 0,
        amountInput: String,
        title: String,
        category: String,
        type: TransactionType,
        paymentMethod: PaymentMethod,
        fromAccount: String? = null,
        toAccount: String? = null,
        dateMillis: Long = System.currentTimeMillis(),
        status: TransactionStatus = TransactionStatus.COMPLETED,
        notes: String = "",
        isWorkExpense: Boolean = false,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _uiEvent.emit("Please enter a title or description")
                return@launch
            }
            if (!MoneyUtils.isValidAmount(amountInput)) {
                _uiEvent.emit("Please enter a valid amount greater than ₹0")
                return@launch
            }

            val amountPaise = MoneyUtils.parseRupeesToPaise(amountInput)

            val transaction = TransactionEntity(
                id = id,
                amountPaise = amountPaise,
                title = title.trim(),
                category = category,
                type = type,
                paymentMethod = PaymentMethod.UPI,
                fromAccount = fromAccount,
                toAccount = toAccount,
                dateMillis = dateMillis,
                status = status,
                notes = notes.trim(),
                isWorkExpense = isWorkExpense
            )

            if (id == 0L) {
                repository.insertTransaction(transaction)
                _uiEvent.emit("Transaction saved")
            } else {
                repository.updateTransaction(transaction)
                _uiEvent.emit("Transaction updated")
            }
            onSuccess()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _uiEvent.emit("Transaction deleted")
        }
    }

    // Statement Import Workflows
    fun resetStatementImportState() {
        _statementImportState.value = StatementImportState.Idle
    }

    fun processStatementFile(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _statementImportState.value = StatementImportState.Processing("Reading and analyzing statement file locally...")

            val parseResult = UniversalStatementParser.parseStatement(context, uri, fileName)
            if (parseResult.candidates.isEmpty()) {
                _statementImportState.value = StatementImportState.Error(parseResult.errorMessage ?: "No transactions found in the file.")
                return@launch
            }

            _statementImportState.value = StatementImportState.Processing("Checking for duplicate transactions...")

            val existingTxs = allTransactions.value
            val analyzedCandidates = DuplicateDetectionService.analyzeDuplicates(parseResult.candidates, existingTxs)

            val reconStatus = BalanceReconciliationService.checkReconciliation(analyzedCandidates)

            val totalSelected = analyzedCandidates.count { it.isSelected }
            val totalCredit = analyzedCandidates.filter { it.isSelected && it.isIncome }.sumOf { it.creditPaise }
            val totalDebit = analyzedCandidates.filter { it.isSelected && !it.isIncome }.sumOf { it.debitPaise }
            val likelyDupes = analyzedCandidates.count { it.duplicateConfidence == DuplicateConfidence.LIKELY_DUPLICATE }
            val possibleDupes = analyzedCandidates.count { it.duplicateConfidence == DuplicateConfidence.POSSIBLE_DUPLICATE }
            val needsReview = analyzedCandidates.count { it.needsReview }

            val summary = StatementImportSummary(
                bankName = parseResult.bankName,
                fileName = fileName,
                totalCandidates = analyzedCandidates.size,
                totalSelected = totalSelected,
                totalCreditPaise = totalCredit,
                totalDebitPaise = totalDebit,
                likelyDuplicateCount = likelyDupes,
                possibleDuplicateCount = possibleDupes,
                needsReviewCount = needsReview,
                reconciliationStatus = reconStatus
            )

            _statementImportState.value = StatementImportState.Reviewing(
                candidates = analyzedCandidates,
                summary = summary
            )
        }
    }

    fun toggleCandidateSelection(candidateId: String) {
        val currentState = _statementImportState.value
        if (currentState is StatementImportState.Reviewing) {
            val updatedList = currentState.candidates.map { c ->
                if (c.id == candidateId) c.copy(isSelected = !c.isSelected) else c
            }
            updateReviewingState(currentState, updatedList)
        }
    }

    fun selectAllCandidates(select: Boolean) {
        val currentState = _statementImportState.value
        if (currentState is StatementImportState.Reviewing) {
            val updatedList = currentState.candidates.map { c -> c.copy(isSelected = select) }
            updateReviewingState(currentState, updatedList)
        }
    }

    fun deselectLikelyDuplicates() {
        val currentState = _statementImportState.value
        if (currentState is StatementImportState.Reviewing) {
            val updatedList = currentState.candidates.map { c ->
                if (c.duplicateConfidence == DuplicateConfidence.LIKELY_DUPLICATE) c.copy(isSelected = false) else c
            }
            updateReviewingState(currentState, updatedList)
        }
    }

    fun updateCandidate(updatedCandidate: CandidateTransaction) {
        val currentState = _statementImportState.value
        if (currentState is StatementImportState.Reviewing) {
            val updatedList = currentState.candidates.map { c ->
                if (c.id == updatedCandidate.id) updatedCandidate else c
            }
            updateReviewingState(currentState, updatedList)
        }
    }

    private fun updateReviewingState(
        currentState: StatementImportState.Reviewing,
        updatedList: List<CandidateTransaction>
    ) {
        val selected = updatedList.filter { it.isSelected }
        val totalCredit = selected.filter { it.isIncome }.sumOf { it.creditPaise }
        val totalDebit = selected.filter { !it.isIncome }.sumOf { it.debitPaise }

        val newSummary = currentState.summary.copy(
            totalSelected = selected.size,
            totalCreditPaise = totalCredit,
            totalDebitPaise = totalDebit
        )

        _statementImportState.value = StatementImportState.Reviewing(
            candidates = updatedList,
            summary = newSummary
        )
    }

    fun confirmAndSaveImportedTransactions() {
        val currentState = _statementImportState.value
        if (currentState !is StatementImportState.Reviewing) return

        viewModelScope.launch {
            _statementImportState.value = StatementImportState.Processing("Saving imported transactions...")

            val selectedCandidates = currentState.candidates.filter { it.isSelected }
            if (selectedCandidates.isEmpty()) {
                _statementImportState.value = StatementImportState.Error("No transactions selected for import.")
                return@launch
            }

            var importedCount = 0
            var totalCredit = 0L
            var totalDebit = 0L

            val bankName = currentState.summary.bankName

            for (candidate in selectedCandidates) {
                val isIncome = candidate.isIncome
                val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                val notesStr = buildString {
                    if (candidate.notes.isNotBlank()) {
                        append(candidate.notes)
                        append(" | ")
                    }
                    append("Imported from $bankName.")
                    if (!candidate.refNumber.isNullOrBlank()) {
                        append(" Ref #${candidate.refNumber}")
                    }
                }

                val tx = TransactionEntity(
                    amountPaise = candidate.amountPaise,
                    title = candidate.description, // User-edited Display Name
                    category = candidate.category,
                    type = type,
                    paymentMethod = candidate.paymentMethod,
                    dateMillis = candidate.dateMillis,
                    status = TransactionStatus.COMPLETED,
                    notes = notesStr,
                    originalDescription = candidate.originalDescription,
                    bankReference = candidate.refNumber
                )

                repository.insertTransaction(tx)
                importedCount++
                if (isIncome) totalCredit += candidate.amountPaise else totalDebit += candidate.amountPaise
            }

            _statementImportState.value = StatementImportState.Success(
                importedCount = importedCount,
                totalCreditPaise = totalCredit,
                totalDebitPaise = totalDebit
            )
        }
    }

    // Work Shift Management
    fun saveWorkShift(
        id: Long = 0,
        platform: String,
        grossInput: String,
        incentivesInput: String,
        hoursWorkedInput: String,
        expensesInput: String,
        dateMillis: Long = System.currentTimeMillis(),
        notes: String = "",
        autoAddIncomeTransaction: Boolean = true,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (platform.isBlank()) {
                _uiEvent.emit("Please enter a platform or source name")
                return@launch
            }
            if (!MoneyUtils.isValidAmount(grossInput)) {
                _uiEvent.emit("Please enter valid gross earnings")
                return@launch
            }

            val grossPaise = MoneyUtils.parseRupeesToPaise(grossInput)
            val incentivesPaise = MoneyUtils.parseRupeesToPaise(incentivesInput)
            val expensesPaise = MoneyUtils.parseRupeesToPaise(expensesInput)
            val hoursWorked = hoursWorkedInput.toDoubleOrNull() ?: 0.0

            var linkedTxId: Long? = null

            // If auto-add income is selected and this is a new shift
            if (autoAddIncomeTransaction && id == 0L) {
                val netProfit = (grossPaise + incentivesPaise) - expensesPaise
                if (netProfit > 0) {
                    val tx = TransactionEntity(
                        amountPaise = netProfit,
                        title = "$platform Work Income",
                        category = "Work Income",
                        type = TransactionType.INCOME,
                        paymentMethod = PaymentMethod.UPI,
                        dateMillis = dateMillis,
                        notes = "Auto-added from work earnings shift"
                    )
                    linkedTxId = repository.insertTransaction(tx)
                }
            } else if (id != 0L) {
                // If editing existing shift, update linked income transaction date & amount if present
                val existingShifts = allWorkShifts.value
                val existingShift = existingShifts.find { it.id == id }
                if (existingShift?.linkedTransactionId != null) {
                    linkedTxId = existingShift.linkedTransactionId
                    val existingTx = repository.getTransactionById(linkedTxId)
                    if (existingTx != null) {
                        val netProfit = (grossPaise + incentivesPaise) - expensesPaise
                        val updatedTx = existingTx.copy(
                            amountPaise = netProfit,
                            title = "$platform Work Income",
                            dateMillis = dateMillis
                        )
                        repository.updateTransaction(updatedTx)
                    }
                }
            }

            val shift = WorkShiftEntity(
                id = id,
                platform = platform.trim(),
                dateMillis = dateMillis,
                grossEarningsPaise = grossPaise,
                incentivesPaise = incentivesPaise,
                hoursWorked = hoursWorked,
                expensesPaise = expensesPaise,
                notes = notes.trim(),
                linkedTransactionId = linkedTxId
            )

            if (id == 0L) {
                repository.insertWorkShift(shift)
                _uiEvent.emit("Work shift saved")
            } else {
                repository.updateWorkShift(shift)
                _uiEvent.emit("Work shift updated")
            }
            onSuccess()
        }
    }

    fun deleteWorkShift(shift: WorkShiftEntity) {
        viewModelScope.launch {
            repository.deleteWorkShift(shift)
            _uiEvent.emit("Work shift deleted")
        }
    }

    // Budget Management
    fun saveBudget(
        id: Long = 0,
        category: String,
        limitAmountInput: String,
        notes: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (category.isBlank()) {
                _uiEvent.emit("Please enter a category name")
                return@launch
            }
            if (!MoneyUtils.isValidAmount(limitAmountInput)) {
                _uiEvent.emit("Please enter a valid monthly limit amount")
                return@launch
            }

            val limitPaise = MoneyUtils.parseRupeesToPaise(limitAmountInput)

            val budget = BudgetEntity(
                id = id,
                category = category.trim(),
                limitAmountPaise = limitPaise,
                notes = notes.trim()
            )

            if (id == 0L) {
                repository.insertBudget(budget)
                _uiEvent.emit("Budget limit set for $category")
            } else {
                repository.updateBudget(budget)
                _uiEvent.emit("Budget updated for $category")
            }
            onSuccess()
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            _uiEvent.emit("Budget deleted")
        }
    }

    // Savings Goal Management
    fun saveSavingsGoal(
        id: Long = 0,
        title: String,
        targetAmountInput: String,
        currentSavedInput: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _uiEvent.emit("Please enter a goal title")
                return@launch
            }
            if (!MoneyUtils.isValidAmount(targetAmountInput)) {
                _uiEvent.emit("Please enter a valid target amount")
                return@launch
            }

            val targetPaise = MoneyUtils.parseRupeesToPaise(targetAmountInput)
            val currentSavedPaise = MoneyUtils.parseRupeesToPaise(currentSavedInput)

            val goal = SavingsGoalEntity(
                id = id,
                title = title.trim(),
                targetAmountPaise = targetPaise,
                currentSavedPaise = currentSavedPaise,
                notes = notes.trim()
            )

            if (id == 0L) {
                repository.insertSavingsGoal(goal)
                _uiEvent.emit("Savings goal created")
            } else {
                repository.updateSavingsGoal(goal)
                _uiEvent.emit("Savings goal updated")
            }
            onSuccess()
        }
    }

    fun depositToSavingsGoal(goal: SavingsGoalEntity, depositInput: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (!MoneyUtils.isValidAmount(depositInput)) {
                _uiEvent.emit("Enter a valid deposit amount")
                return@launch
            }
            val depositPaise = MoneyUtils.parseRupeesToPaise(depositInput)
            val updated = goal.copy(currentSavedPaise = goal.currentSavedPaise + depositPaise)
            repository.updateSavingsGoal(updated)
            _uiEvent.emit("Deposit recorded for ${goal.title}")
            onSuccess()
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            _uiEvent.emit("Savings goal deleted")
        }
    }

    // Debts & Repayments
    fun saveDebt(
        id: Long = 0,
        personName: String,
        amountInput: String,
        type: DebtType,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (personName.isBlank()) {
                _uiEvent.emit("Please enter person name")
                return@launch
            }
            if (!MoneyUtils.isValidAmount(amountInput)) {
                _uiEvent.emit("Please enter a valid amount")
                return@launch
            }

            val amountPaise = MoneyUtils.parseRupeesToPaise(amountInput)

            val debt = DebtEntity(
                id = id,
                personName = personName.trim(),
                amountPaise = amountPaise,
                type = type,
                notes = notes.trim()
            )

            if (id == 0L) {
                repository.insertDebt(debt)
                _uiEvent.emit("Money record added")
            } else {
                repository.updateDebt(debt)
                _uiEvent.emit("Money record updated")
            }
            onSuccess()
        }
    }

    fun addDebtRepayment(debt: DebtEntity, repaymentAmountInput: String, paymentMethod: PaymentMethod, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (!MoneyUtils.isValidAmount(repaymentAmountInput)) {
                _uiEvent.emit("Enter a valid repayment amount")
                return@launch
            }
            val paise = MoneyUtils.parseRupeesToPaise(repaymentAmountInput)
            val repayment = DebtRepaymentEntity(
                debtId = debt.id,
                amountPaise = paise,
                paymentMethod = paymentMethod,
                notes = "Repayment for ${debt.personName}"
            )
            repository.addDebtRepayment(debt, repayment)
            _uiEvent.emit("Repayment recorded")
            onSuccess()
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            _uiEvent.emit("Money record deleted")
        }
    }

    // Custom Categories
    fun saveCategory(name: String, type: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiEvent.emit("Category name cannot be empty")
                return@launch
            }
            val cat = CategoryEntity(name = name.trim(), type = type, isDefault = false)
            repository.insertCategory(cat)
            _uiEvent.emit("Category created")
            onSuccess()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.isDefault) {
                _uiEvent.emit("Default categories cannot be deleted")
                return@launch
            }
            repository.deleteCategory(category)
            _uiEvent.emit("Category removed")
        }
    }

    // Investments
    val allInvestmentHoldings: StateFlow<List<InvestmentHoldingEntity>> = repository.allInvestmentHoldings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvestmentActivities: StateFlow<List<InvestmentActivityEntity>> = repository.allInvestmentActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveInvestmentHolding(
        id: Long = 0L,
        name: String,
        type: String,
        provider: String?,
        accountReference: String?,
        initialAmountInput: String,
        unitsInput: String,
        navInput: String,
        currentValueInput: String,
        schemeCode: String? = null,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiEvent.emit("Please enter investment name")
                return@launch
            }
            if (type.isBlank()) {
                _uiEvent.emit("Please enter investment type")
                return@launch
            }
            if (!MoneyUtils.isValidAmount(initialAmountInput)) {
                _uiEvent.emit("Please enter a valid initial amount")
                return@launch
            }

            val initialPaise = MoneyUtils.parseRupeesToPaise(initialAmountInput)
            val units = unitsInput.toDoubleOrNull() ?: 0.0
            val nav = navInput.toDoubleOrNull() ?: 0.0
            val currentPaise = if (MoneyUtils.isValidAmount(currentValueInput)) {
                MoneyUtils.parseRupeesToPaise(currentValueInput)
            } else if (units > 0.0 && nav > 0.0) {
                (units * nav * 100.0).toLong()
            } else {
                initialPaise
            }

            val holding = InvestmentHoldingEntity(
                id = id,
                name = name.trim(),
                type = type.trim(),
                provider = provider?.trim()?.ifBlank { null },
                accountReference = accountReference?.trim()?.ifBlank { null },
                investedCostPaise = initialPaise,
                currentValuePaise = currentPaise,
                units = units,
                nav = nav,
                schemeCode = schemeCode?.trim()?.ifBlank { null },
                lastValuationDateMillis = System.currentTimeMillis(),
                notes = notes.trim()
            )

            if (id == 0L) {
                val newId = repository.insertInvestmentHolding(holding)
                val tx = TransactionEntity(
                    amountPaise = initialPaise,
                    title = "Investment Purchase: $name",
                    category = "Investment",
                    type = TransactionType.EXPENSE,
                    paymentMethod = PaymentMethod.UPI,
                    notes = "Initial investment in $type"
                )
                val txId = repository.insertTransaction(tx)

                val activity = InvestmentActivityEntity(
                    holdingId = newId,
                    activityType = InvestmentActivityType.PURCHASE,
                    amountPaise = initialPaise,
                    units = units,
                    nav = nav,
                    linkedTransactionId = txId,
                    notes = "Initial purchase"
                )
                repository.insertInvestmentActivity(activity)
            } else {
                repository.updateInvestmentHolding(holding)
            }

            _uiEvent.emit("Investment saved successfully")
            onSuccess()
        }
    }

    fun recordInvestmentActivity(
        holding: InvestmentHoldingEntity,
        activityType: InvestmentActivityType,
        amountInput: String,
        unitsInput: String,
        navInput: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (!MoneyUtils.isValidAmount(amountInput) && activityType != InvestmentActivityType.VALUATION_UPDATE) {
                _uiEvent.emit("Please enter a valid amount")
                return@launch
            }

            val amountPaise = MoneyUtils.parseRupeesToPaise(amountInput)
            val units = unitsInput.toDoubleOrNull() ?: 0.0
            val nav = navInput.toDoubleOrNull() ?: 0.0

            var newCost = holding.investedCostPaise
            var newCurrent = holding.currentValuePaise
            var newUnits = holding.units

            var linkedTxId: Long? = null

            when (activityType) {
                InvestmentActivityType.PURCHASE -> {
                    newCost += amountPaise
                    newCurrent += amountPaise
                    newUnits += units

                    val tx = TransactionEntity(
                        amountPaise = amountPaise,
                        title = "Investment Purchase: ${holding.name}",
                        category = "Investment",
                        type = TransactionType.EXPENSE,
                        paymentMethod = PaymentMethod.UPI,
                        notes = notes.ifBlank { "Additional purchase" }
                    )
                    linkedTxId = repository.insertTransaction(tx)
                }
                InvestmentActivityType.REDEMPTION -> {
                    if (amountPaise > holding.currentValuePaise) {
                        _uiEvent.emit("Redemption amount cannot exceed current holding value")
                        return@launch
                    }
                    val proportion = if (holding.currentValuePaise > 0) amountPaise.toDouble() / holding.currentValuePaise.toDouble() else 1.0
                    val costReduction = (holding.investedCostPaise * proportion).toLong()

                    newCost = (holding.investedCostPaise - costReduction).coerceAtLeast(0L)
                    newCurrent = (holding.currentValuePaise - amountPaise).coerceAtLeast(0L)
                    newUnits = (holding.units - (units.takeIf { it > 0.0 } ?: (holding.units * proportion))).coerceAtLeast(0.0)

                    val tx = TransactionEntity(
                        amountPaise = amountPaise,
                        title = "Investment Redemption: ${holding.name}",
                        category = "Investment",
                        type = TransactionType.INCOME,
                        paymentMethod = PaymentMethod.UPI,
                        notes = notes.ifBlank { "Redemption proceeds" }
                    )
                    linkedTxId = repository.insertTransaction(tx)
                }
                InvestmentActivityType.DIVIDEND -> {
                    val tx = TransactionEntity(
                        amountPaise = amountPaise,
                        title = "Investment Dividend: ${holding.name}",
                        category = "Investment Income",
                        type = TransactionType.INCOME,
                        paymentMethod = PaymentMethod.UPI,
                        notes = notes.ifBlank { "Dividend payout" }
                    )
                    linkedTxId = repository.insertTransaction(tx)
                }
                InvestmentActivityType.VALUATION_UPDATE -> {
                    newCurrent = amountPaise
                    if (units > 0.0) newUnits = units
                }
            }

            val updatedHolding = holding.copy(
                investedCostPaise = newCost,
                currentValuePaise = newCurrent,
                units = newUnits,
                nav = if (nav > 0.0) nav else holding.nav,
                lastValuationDateMillis = System.currentTimeMillis()
            )

            repository.updateInvestmentHolding(updatedHolding)

            val activity = InvestmentActivityEntity(
                holdingId = holding.id,
                activityType = activityType,
                amountPaise = amountPaise,
                units = units,
                nav = nav,
                linkedTransactionId = linkedTxId,
                notes = notes.trim()
            )
            repository.insertInvestmentActivity(activity)

            _uiEvent.emit("Investment activity recorded")
            onSuccess()
        }
    }

    fun deleteInvestmentHolding(holding: InvestmentHoldingEntity) {
        viewModelScope.launch {
            repository.deleteInvestmentHolding(holding)
            _uiEvent.emit("Investment deleted")
        }
    }

    // Backup & CSV Export
    fun getCsvData(): String {
        return CsvExporter.exportTransactionsToCsv(allTransactions.value)
    }

    suspend fun getBackupJson(): String {
        return BackupRestoreManager.createBackupJson(
            transactions = allTransactions.value,
            categories = allCategories.value,
            workShifts = allWorkShifts.value,
            savingsGoals = allSavingsGoals.value,
            debts = allDebts.value
        )
    }

    fun restoreFromBackupJson(jsonString: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = BackupRestoreManager.restoreFromJson(jsonString, repository)
            if (success) {
                _uiEvent.emit("Backup restored successfully!")
                onSuccess()
            } else {
                _uiEvent.emit("Failed to restore backup. Invalid file format.")
            }
        }
    }
}
