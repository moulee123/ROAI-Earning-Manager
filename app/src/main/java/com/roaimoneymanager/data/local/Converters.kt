package com.roaimoneymanager.data.local

import androidx.room.TypeConverter
import com.roaimoneymanager.domain.model.DebtStatus
import com.roaimoneymanager.domain.model.DebtType
import com.roaimoneymanager.domain.model.PaymentMethod
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus): String = value.name

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus = try {
        TransactionStatus.valueOf(value)
    } catch (e: Exception) {
        TransactionStatus.COMPLETED
    }

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType = try {
        DebtType.valueOf(value)
    } catch (e: Exception) {
        DebtType.LENT
    }

    @TypeConverter
    fun fromDebtStatus(value: DebtStatus): String = value.name

    @TypeConverter
    fun toDebtStatus(value: String): DebtStatus = try {
        DebtStatus.valueOf(value)
    } catch (e: Exception) {
        DebtStatus.PENDING
    }
}
