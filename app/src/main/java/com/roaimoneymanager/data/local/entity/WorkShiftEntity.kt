package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_shifts")
data class WorkShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: String, // e.g., Swiggy, Zomato, Uber, Dunzo, Amazon Flex, Freelance, Other
    val dateMillis: Long = System.currentTimeMillis(),
    val grossEarningsPaise: Long,
    val incentivesPaise: Long = 0L,
    val hoursWorked: Double = 0.0,
    val expensesPaise: Long = 0L, // petrol, maintenance, etc.
    val notes: String = "",
    val linkedTransactionId: Long? = null
) {
    val netProfitPaise: Long
        get() = (grossEarningsPaise + incentivesPaise) - expensesPaise

    val netHourlyEarningsPaise: Long
        get() = if (hoursWorked > 0) (netProfitPaise.toDouble() / hoursWorked).toLong() else 0L
}
