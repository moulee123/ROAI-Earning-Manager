package com.roaimoneymanager.data.local.dao

import androidx.room.*
import com.roaimoneymanager.data.local.entity.WorkShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkShiftDao {

    @Query("SELECT * FROM work_shifts ORDER BY dateMillis DESC")
    fun getAllWorkShifts(): Flow<List<WorkShiftEntity>>

    @Query("SELECT * FROM work_shifts WHERE dateMillis BETWEEN :startDate AND :endDate ORDER BY dateMillis DESC")
    fun getWorkShiftsBetween(startDate: Long, endDate: Long): Flow<List<WorkShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkShift(shift: WorkShiftEntity): Long

    @Update
    suspend fun updateWorkShift(shift: WorkShiftEntity)

    @Delete
    suspend fun deleteWorkShift(shift: WorkShiftEntity)
}
