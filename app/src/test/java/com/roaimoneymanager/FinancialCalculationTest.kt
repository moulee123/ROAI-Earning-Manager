package com.roaimoneymanager

import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.data.local.entity.WorkShiftEntity
import com.roaimoneymanager.data.preferences.OpeningBalances
import com.roaimoneymanager.domain.calculator.FinancialCalculator
import com.roaimoneymanager.domain.model.PaymentMethod
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.util.MoneyUtils
import org.junit.Assert.*
import org.junit.Test

class FinancialCalculationTest {

    @Test
    fun testOpeningBalanceExactPrecision() {
        // ₹128.67 must parse to 12867 Long paise
        val paise12867 = MoneyUtils.parseRupeesToPaise("128.67")
        assertEquals(12867L, paise12867)

        val formatted12867 = MoneyUtils.formatPaiseToRupees(12867L, showSymbol = true)
        assertEquals("₹128.67", formatted12867)

        // ₹0.01 must parse to 1 Long paise
        val paise001 = MoneyUtils.parseRupeesToPaise("0.01")
        assertEquals(1L, paise001)

        val formatted001 = MoneyUtils.formatPaiseToRupees(1L, showSymbol = true)
        assertEquals("₹0.01", formatted001)

        // ₹100.99 must parse to 10099 Long paise
        val paise10099 = MoneyUtils.parseRupeesToPaise("100.99")
        assertEquals(10099L, paise10099)

        val formatted10099 = MoneyUtils.formatPaiseToRupees(10099L, showSymbol = true)
        assertEquals("₹100.99", formatted10099)
    }

    @Test
    fun testMultipleOpeningBalancesAndTotalAvailable() {
        val openingBalances = OpeningBalances(
            cashPaise = MoneyUtils.parseRupeesToPaise("128.67"), // 12867L
            bankPaise = MoneyUtils.parseRupeesToPaise("100.99"), // 10099L
            upiPaise = MoneyUtils.parseRupeesToPaise("0.01")     // 1L
        )

        // Sum = 128.67 + 100.99 + 0.01 = 229.67 (22967L paise)
        assertEquals(22967L, openingBalances.totalOpeningPaise)
        assertEquals("₹229.67", MoneyUtils.formatPaiseToRupees(openingBalances.totalOpeningPaise))

        val now = System.currentTimeMillis()

        val transactions = listOf(
            // Income: ₹128.67
            TransactionEntity(
                id = 1,
                amountPaise = MoneyUtils.parseRupeesToPaise("128.67"),
                title = "Work Income",
                category = "Income",
                type = TransactionType.INCOME,
                paymentMethod = PaymentMethod.BANK,
                status = TransactionStatus.COMPLETED,
                dateMillis = now
            ),
            // Expense: ₹0.01
            TransactionEntity(
                id = 2,
                amountPaise = MoneyUtils.parseRupeesToPaise("0.01"),
                title = "Fee",
                category = "Bills",
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.UPI,
                status = TransactionStatus.COMPLETED,
                dateMillis = now
            ),
            // Internal Transfer: ₹50.00 (Must NOT change total balance or count as income/expense)
            TransactionEntity(
                id = 3,
                amountPaise = MoneyUtils.parseRupeesToPaise("50.00"),
                title = "Bank to Cash",
                category = "Transfer",
                type = TransactionType.TRANSFER,
                paymentMethod = PaymentMethod.BANK,
                status = TransactionStatus.COMPLETED,
                dateMillis = now
            )
        )

        val summary = FinancialCalculator.calculateSummary(
            transactions = transactions,
            openingBalances = openingBalances,
            todayStartMillis = now - 1000,
            todayEndMillis = now + 1000,
            monthStartMillis = now - 10000,
            monthEndMillis = now + 10000
        )

        // Total Income = ₹128.67 (12867L)
        assertEquals(12867L, summary.totalIncomePaise)

        // Total Expense = ₹0.01 (1L)
        assertEquals(1L, summary.totalExpensesPaise)

        // Available Balance = 22967 + 12867 - 1 = 35833L (₹358.33)
        assertEquals(35833L, summary.availableBalancePaise)
        assertEquals("₹358.33", MoneyUtils.formatPaiseToRupees(summary.availableBalancePaise))
    }

    @Test
    fun testBackdatedTransactionAndDateEditing() {
        val openingBalances = OpeningBalances(cashPaise = 0L, bankPaise = 0L, upiPaise = 0L)
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - (10 * 24 * 60 * 60 * 1000L)

        // 1. Backdated Expense added with date = 10 days ago
        val backdatedTx = TransactionEntity(
            id = 1,
            amountPaise = MoneyUtils.parseRupeesToPaise("250.00"),
            title = "College Books",
            category = "College",
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CASH,
            status = TransactionStatus.COMPLETED,
            dateMillis = tenDaysAgo
        )

        val transactionsList = listOf(backdatedTx)

        val todaySummary = FinancialCalculator.calculateSummary(
            transactions = transactionsList,
            openingBalances = openingBalances,
            todayStartMillis = now - (3600 * 1000L),
            todayEndMillis = now + (3600 * 1000L),
            monthStartMillis = now - (30 * 24 * 3600 * 1000L),
            monthEndMillis = now + (30 * 24 * 3600 * 1000L)
        )

        // Today's expense should be 0 because transaction date is 10 days ago
        assertEquals(0L, todaySummary.todayExpensesPaise)

        // Total Expense and Monthly Expense include it
        assertEquals(25000L, todaySummary.totalExpensesPaise)
        assertEquals(25000L, todaySummary.monthlyExpensesPaise)

        // 2. Editing the date of the transaction to today (now)
        val editedTx = backdatedTx.copy(dateMillis = now)
        val updatedList = listOf(editedTx)

        val updatedSummary = FinancialCalculator.calculateSummary(
            transactions = updatedList,
            openingBalances = openingBalances,
            todayStartMillis = now - (3600 * 1000L),
            todayEndMillis = now + (3600 * 1000L),
            monthStartMillis = now - (30 * 24 * 3600 * 1000L),
            monthEndMillis = now + (30 * 24 * 3600 * 1000L)
        )

        // Amount remains unchanged (₹250.00 = 25000L)
        assertEquals(25000L, editedTx.amountPaise)

        // Now today's expense includes it
        assertEquals(25000L, updatedSummary.todayExpensesPaise)

        // Total Expense is still ₹250.00 (not duplicated)
        assertEquals(25000L, updatedSummary.totalExpensesPaise)
    }

    @Test
    fun testWorkShiftCalculations() {
        val shift = WorkShiftEntity(
            id = 1,
            platform = "Swiggy",
            grossEarningsPaise = MoneyUtils.parseRupeesToPaise("800.00"),
            incentivesPaise = MoneyUtils.parseRupeesToPaise("200.00"),
            expensesPaise = MoneyUtils.parseRupeesToPaise("150.00"),
            hoursWorked = 4.0
        )

        // Net Profit = (800 + 200) - 150 = ₹850
        assertEquals(85000L, shift.netProfitPaise)
        assertEquals("₹850.00", MoneyUtils.formatPaiseToRupees(shift.netProfitPaise))

        // Net Hourly Rate = 850 / 4 = ₹212.50
        assertEquals(21250L, shift.netHourlyEarningsPaise)
        assertEquals("₹212.50", MoneyUtils.formatPaiseToRupees(shift.netHourlyEarningsPaise))
    }
}
