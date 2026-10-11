package com.roaimoneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Category",
    val colorHex: Long = 0xFF176B45,
    val type: String = "EXPENSE", // "EXPENSE", "INCOME", "BOTH"
    val isDefault: Boolean = false
)
