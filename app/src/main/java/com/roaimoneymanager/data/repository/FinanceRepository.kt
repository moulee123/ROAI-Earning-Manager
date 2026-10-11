package com.roaimoneymanager.data.repository

import com.roaimoneymanager.data.local.AppDatabase
import com.roaimoneymanager.data.local.entity.*
import com.roaimoneymanager.domain.model.DebtStatus
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val database: AppDatabase) {

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> {
        return database.transactionDao().getRecentTransactions(limit)
    }

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return database.transactionDao().getTransactionsBetween(startDate, endDate)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return database.transactionDao().getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return database.transactionDao().insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.transactionDao().deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        database.transactionDao().deleteTransactionById(id)
    }

    suspend fun deleteAllTransactions() {
        database.transactionDao().deleteAllTransactions()
    }

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = database.categoryDao().getAllCategories()

    suspend fun insertCategory(category: CategoryEntity): Long {
        return database.categoryDao().insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        database.categoryDao().updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        database.categoryDao().deleteCategory(category)
    }

    // Work Shifts
    val allWorkShifts: Flow<List<WorkShiftEntity>> = database.workShiftDao().getAllWorkShifts()

    fun getWorkShiftsBetween(startDate: Long, endDate: Long): Flow<List<WorkShiftEntity>> {
        return database.workShiftDao().getWorkShiftsBetween(startDate, endDate)
    }

    suspend fun insertWorkShift(shift: WorkShiftEntity): Long {
        return database.workShiftDao().insertWorkShift(shift)
    }

    suspend fun updateWorkShift(shift: WorkShiftEntity) {
        database.workShiftDao().updateWorkShift(shift)
    }

    suspend fun deleteWorkShift(shift: WorkShiftEntity) {
        database.workShiftDao().deleteWorkShift(shift)
    }

    // Savings Goals
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = database.savingsGoalDao().getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long {
        return database.savingsGoalDao().insertSavingsGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        database.savingsGoalDao().updateSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        database.savingsGoalDao().deleteSavingsGoal(goal)
    }

    // Debts
    val allDebts: Flow<List<DebtEntity>> = database.debtDao().getAllDebts()

    suspend fun insertDebt(debt: DebtEntity): Long {
        return database.debtDao().insertDebt(debt)
    }

    suspend fun updateDebt(debt: DebtEntity) {
        database.debtDao().updateDebt(debt)
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        database.debtDao().deleteDebt(debt)
    }

    suspend fun addDebtRepayment(debt: DebtEntity, repayment: DebtRepaymentEntity) {
        database.debtDao().insertDebtRepayment(repayment)
        val updatedPaidPaise = debt.paidPaise + repayment.amountPaise
        val updatedStatus = if (updatedPaidPaise >= debt.amountPaise) DebtStatus.SETTLED else DebtStatus.PENDING
        val updatedDebt = debt.copy(
            paidPaise = updatedPaidPaise,
            status = updatedStatus
        )
        database.debtDao().updateDebt(updatedDebt)
    }

    fun getRepaymentsForDebt(debtId: Long): Flow<List<DebtRepaymentEntity>> {
        return database.debtDao().getRepaymentsForDebt(debtId)
    }
}
