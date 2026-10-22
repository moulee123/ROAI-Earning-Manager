package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investment_holdings")
data class InvestmentHoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // Custom investment type (e.g. Mutual Funds, Gold, Stocks, Real Estate, etc.)
    val provider: String? = null, // Broker, fund house, bank, platform
    val accountReference: String? = null, // Folio / account number / ID
    val investedCostPaise: Long = 0L,
    val currentValuePaise: Long = 0L,
    val units: Double = 0.0,
    val nav: Double = 0.0,
    val schemeCode: String? = null,
    val lastValuationDateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val unrealizedProfitLossPaise: Long
        get() = currentValuePaise - investedCostPaise

    val unrealizedReturnPercentage: Double
        get() = if (investedCostPaise > 0L) {
            (unrealizedProfitLossPaise.toDouble() / investedCostPaise.toDouble()) * 100.0
        } else {
            0.0
        }

    val isFullyRedeemed: Boolean
        get() = units <= 0.0 && currentValuePaise <= 0L && investedCostPaise <= 0L
}
