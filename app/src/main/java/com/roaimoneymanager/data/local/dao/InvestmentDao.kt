package com.roaimoneymanager.data.local.dao

import androidx.room.*
import com.roaimoneymanager.data.local.entity.InvestmentActivityEntity
import com.roaimoneymanager.data.local.entity.InvestmentHoldingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {

    @Query("SELECT * FROM investment_holdings ORDER BY currentValuePaise DESC")
    fun getAllHoldings(): Flow<List<InvestmentHoldingEntity>>

    @Query("SELECT * FROM investment_holdings")
    suspend fun getAllHoldingsSync(): List<InvestmentHoldingEntity>

    @Query("SELECT * FROM investment_holdings WHERE id = :id")
    suspend fun getHoldingById(id: Long): InvestmentHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: InvestmentHoldingEntity): Long

    @Update
    suspend fun updateHolding(holding: InvestmentHoldingEntity)

    @Delete
    suspend fun deleteHolding(holding: InvestmentHoldingEntity)

    @Query("SELECT * FROM investment_activities WHERE holdingId = :holdingId ORDER BY dateMillis DESC")
    fun getActivitiesForHolding(holdingId: Long): Flow<List<InvestmentActivityEntity>>

    @Query("SELECT * FROM investment_activities ORDER BY dateMillis DESC")
    fun getAllActivities(): Flow<List<InvestmentActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: InvestmentActivityEntity): Long

    @Delete
    suspend fun deleteActivity(activity: InvestmentActivityEntity)
}
