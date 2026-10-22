package com.roaimoneymanager

import com.roaimoneymanager.data.local.entity.InvestmentHoldingEntity
import com.roaimoneymanager.util.MoneyUtils
import org.junit.Assert.*
import org.junit.Test

class InvestmentCalculationTest {

    @Test
    fun testInvestmentProfitLossAndReturnPercentage() {
        val holding = InvestmentHoldingEntity(
            id = 1,
            name = "Nifty 50 Index Fund",
            type = "Mutual Funds",
            investedCostPaise = MoneyUtils.parseRupeesToPaise("10000.00"), // ₹10,000 (1000000 paise)
            currentValuePaise = MoneyUtils.parseRupeesToPaise("12500.00"), // ₹12,500 (1250000 paise)
            units = 100.0,
            nav = 125.0
        )

        // Unrealized Profit = 12500 - 10000 = ₹2,500 (250000 paise)
        assertEquals(250000L, holding.unrealizedProfitLossPaise)

        // Return Percentage = (2500 / 10000) * 100 = 25.0%
        assertEquals(25.0, holding.unrealizedReturnPercentage, 0.001)
    }

    @Test
    fun testZeroCostBasisAndFullyRedeemed() {
        val zeroCostHolding = InvestmentHoldingEntity(
            id = 2,
            name = "Free Bonus Shares",
            type = "Stocks",
            investedCostPaise = 0L,
            currentValuePaise = MoneyUtils.parseRupeesToPaise("5000.00"),
            units = 50.0,
            nav = 100.0
        )

        // Unrealized profit = ₹5,000
        assertEquals(500000L, zeroCostHolding.unrealizedProfitLossPaise)

        // Return percentage with zero cost basis should not divide by zero
        assertEquals(0.0, zeroCostHolding.unrealizedReturnPercentage, 0.001)

        val redeemedHolding = InvestmentHoldingEntity(
            id = 3,
            name = "Closed Fund",
            type = "Mutual Funds",
            investedCostPaise = 0L,
            currentValuePaise = 0L,
            units = 0.0,
            nav = 0.0
        )
        assertTrue(redeemedHolding.isFullyRedeemed)
    }

    @Test
    fun testPartialRedemptionProportionalCostBasis() {
        val initialInvestedPaise = MoneyUtils.parseRupeesToPaise("20000.00") // ₹20,000
        val initialCurrentPaise = MoneyUtils.parseRupeesToPaise("25000.00")  // ₹25,000
        val redemptionAmountPaise = MoneyUtils.parseRupeesToPaise("5000.00") // ₹5,000 redeemed

        val proportion = redemptionAmountPaise.toDouble() / initialCurrentPaise.toDouble() // 5000 / 25000 = 0.2 (20%)
        val costReduction = (initialInvestedPaise * proportion).toLong() // 20000 * 0.2 = ₹4,000 cost basis reduction

        val remainingCost = initialInvestedPaise - costReduction // 20000 - 4000 = ₹16,000
        val remainingValue = initialCurrentPaise - redemptionAmountPaise // 25000 - 5000 = ₹20,000

        assertEquals(1600000L, remainingCost)
        assertEquals(2000000L, remainingValue)
    }
}
