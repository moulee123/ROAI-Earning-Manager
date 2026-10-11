package com.roaimoneymanager.domain.calculator

import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.data.preferences.OpeningBalances
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType

data class FinancialSummary(
    val totalIncomePaise: Long = 0L,
    val totalExpensesPaise: Long = 0L,
    val netCashFlowPaise: Long = 0L,
    val totalOpeningPaise: Long = 0L,
    val availableBalancePaise: Long = 0L,
    val todayIncomePaise: Long = 0L,
    val todayExpensesPaise: Long = 0L,
    val monthlyIncomePaise: Long = 0L,
    val monthlyExpensesPaise: Long = 0L,
    val monthlyNetSavingsPaise: Long = 0L,
    val pendingIncomePaise: Long = 0L,
    val pendingExpensePaise: Long = 0L
)

object FinancialCalculator {

    fun calculateSummary(
        transactions: List<TransactionEntity>,
        openingBalances: OpeningBalances,
        todayStartMillis: Long,
        todayEndMillis: Long,
        monthStartMillis: Long,
        monthEndMillis: Long
    ): FinancialSummary {
        var totalIncome = 0L
        var totalExpenses = 0L
        var todayIncome = 0L
        var todayExpenses = 0L
        var monthlyIncome = 0L
        var monthlyExpenses = 0L
        var pendingIncome = 0L
        var pendingExpense = 0L

        var actualInflows = 0L
        var actualOutflows = 0L

        for (tx in transactions) {
            // Pending payments must NOT be included in available cash or completed income/expense
            if (tx.status == TransactionStatus.PENDING) {
                when (tx.type) {
                    TransactionType.INCOME -> pendingIncome += tx.amountPaise
                    TransactionType.EXPENSE -> pendingExpense += tx.amountPaise
                    TransactionType.TRANSFER -> {}
                }
                continue
            }

            // Completed transactions processing:
            when (tx.type) {
                TransactionType.INCOME -> {
                    totalIncome += tx.amountPaise
                    actualInflows += tx.amountPaise

                    if (tx.dateMillis in todayStartMillis..todayEndMillis) {
                        todayIncome += tx.amountPaise
                    }
                    if (tx.dateMillis in monthStartMillis..monthEndMillis) {
                        monthlyIncome += tx.amountPaise
                    }
                }
                TransactionType.EXPENSE -> {
                    totalExpenses += tx.amountPaise
                    actualOutflows += tx.amountPaise

                    if (tx.dateMillis in todayStartMillis..todayEndMillis) {
                        todayExpenses += tx.amountPaise
                    }
                    if (tx.dateMillis in monthStartMillis..monthEndMillis) {
                        monthlyExpenses += tx.amountPaise
                    }
                }
                TransactionType.TRANSFER -> {
                    // Transfers between user's own accounts must NOT count as income or expenses!
                    // Available overall balance is unchanged by internal transfer.
                }
            }
        }

        val netCashFlow = totalIncome - totalExpenses
        val totalOpening = openingBalances.totalOpeningPaise
        val availableBalance = totalOpening + actualInflows - actualOutflows
        val monthlyNetSavings = monthlyIncome - monthlyExpenses

        return FinancialSummary(
            totalIncomePaise = totalIncome,
            totalExpensesPaise = totalExpenses,
            netCashFlowPaise = netCashFlow,
            totalOpeningPaise = totalOpening,
            availableBalancePaise = availableBalance,
            todayIncomePaise = todayIncome,
            todayExpensesPaise = todayExpenses,
            monthlyIncomePaise = monthlyIncome,
            monthlyExpensesPaise = monthlyExpenses,
            monthlyNetSavingsPaise = monthlyNetSavings,
            pendingIncomePaise = pendingIncome,
            pendingExpensePaise = pendingExpense
        )
    }
}
