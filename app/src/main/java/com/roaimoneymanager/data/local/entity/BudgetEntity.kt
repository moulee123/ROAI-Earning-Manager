package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val limitAmountPaise: Long,
    val monthYear: String = "", // e.g. "2026-03" or empty for recurring monthly
    val notes: String = ""
)
