package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InvestmentActivityType {
    PURCHASE,
    REDEMPTION,
    DIVIDEND,
    VALUATION_UPDATE
}

@Entity(tableName = "investment_activities")
data class InvestmentActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val holdingId: Long,
    val activityType: InvestmentActivityType,
    val amountPaise: Long, // Purchase cost, redemption proceeds, or dividend amount
    val units: Double = 0.0,
    val nav: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val linkedTransactionId: Long? = null, // Optional link to transaction ledger
    val notes: String = ""
)
