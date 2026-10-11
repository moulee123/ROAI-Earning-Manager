package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roaimoneymanager.domain.model.DebtStatus
import com.roaimoneymanager.domain.model.DebtType

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val amountPaise: Long,
    val paidPaise: Long = 0L,
    val type: DebtType, // LENT or BORROWED
    val dateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long? = null,
    val status: DebtStatus = DebtStatus.PENDING,
    val notes: String = ""
) {
    val remainingPaise: Long
        get() = (amountPaise - paidPaise).coerceAtLeast(0L)
}
