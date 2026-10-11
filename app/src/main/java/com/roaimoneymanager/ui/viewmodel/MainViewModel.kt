package com.roaimoneymanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roaimoneymanager.data.export.BackupRestoreManager
import com.roaimoneymanager.data.export.CsvExporter
import com.roaimoneymanager.data.local.AppDatabase
import com.roaimoneymanager.data.local.entity.*
import com.roaimoneymanager.data.preferences.OpeningBalances
import com.roaimoneymanager.data.preferences.UserPreferencesRepository
import com.roaimoneymanager.data.repository.FinanceRepository
import com.roaimoneymanager.domain.calculator.FinancialCalculator
import com.roaimoneymanager.domain.calculator.FinancialSummary
import com.roaimoneymanager.domain.model.*
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    val openingBalances: StateFlow<OpeningBalances> = userPreferencesRepository.openingBalancesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OpeningBalances())

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
                paymentMethod = paymentMethod,
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
                        paymentMethod = PaymentMethod.BANK,
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
