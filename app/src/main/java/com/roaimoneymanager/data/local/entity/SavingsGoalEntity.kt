package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmountPaise: Long,
    val currentSavedPaise: Long = 0L,
    val targetDateMillis: Long? = null,
    val colorHex: Long = 0xFF176B45,
    val notes: String = ""
) {
    val progressRatio: Float
        get() = if (targetAmountPaise > 0) (currentSavedPaise.toFloat() / targetAmountPaise.toFloat()).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = currentSavedPaise >= targetAmountPaise
}
