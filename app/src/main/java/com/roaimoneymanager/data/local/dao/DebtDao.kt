package com.roaimoneymanager.data.local.dao

import androidx.room.*
import com.roaimoneymanager.data.local.entity.DebtEntity
import com.roaimoneymanager.data.local.entity.DebtRepaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts ORDER BY dateMillis DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRepayment(repayment: DebtRepaymentEntity): Long

    @Query("SELECT * FROM debt_repayments WHERE debtId = :debtId ORDER BY dateMillis DESC")
    fun getRepaymentsForDebt(debtId: Long): Flow<List<DebtRepaymentEntity>>
}
