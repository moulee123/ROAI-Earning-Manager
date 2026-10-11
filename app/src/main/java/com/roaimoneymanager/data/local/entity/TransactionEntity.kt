package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roaimoneymanager.domain.model.PaymentMethod
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountPaise: Long,
    val title: String,
    val category: String,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val notes: String = "",
    val isWorkExpense: Boolean = false,
    val relatedDebtId: Long? = null
)
