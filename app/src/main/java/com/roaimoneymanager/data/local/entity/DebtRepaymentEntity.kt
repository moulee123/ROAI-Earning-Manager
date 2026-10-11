package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roaimoneymanager.domain.model.PaymentMethod

@Entity(tableName = "debt_repayments")
data class DebtRepaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtId: Long,
    val amountPaise: Long,
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val notes: String = ""
)
