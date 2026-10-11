package com.roaimoneymanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.roaimoneymanager.data.local.dao.*
import com.roaimoneymanager.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        WorkShiftEntity::class,
        SavingsGoalEntity::class,
        DebtEntity::class,
        DebtRepaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun workShiftDao(): WorkShiftDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roai_money_manager_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }

            private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
                val defaults = listOf(
                    CategoryEntity(name = "Food", iconName = "Restaurant", colorHex = 0xFFFF5722, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "Petrol", iconName = "LocalGasStation", colorHex = 0xFFFFC107, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "Transport", iconName = "DirectionsBus", colorHex = 0xFF2196F3, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "College", iconName = "School", colorHex = 0xFF9C27B0, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "Shopping", iconName = "ShoppingBag", colorHex = 0xFFE91E63, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "Bills", iconName = "Receipt", colorHex = 0xFF607D8B, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "Entertainment", iconName = "Movie", colorHex = 0xFF673AB7, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "Work Expense", iconName = "Work", colorHex = 0xFF795548, type = "EXPENSE", isDefault = true),
                    CategoryEntity(name = "Other Expense", iconName = "MoreHoriz", colorHex = 0xFF9E9E9E, type = "EXPENSE", isDefault = true),
                    
                    // Income Categories
                    CategoryEntity(name = "Salary", iconName = "Payments", colorHex = 0xFF4CAF50, type = "INCOME", isDefault = true),
                    CategoryEntity(name = "Work Income", iconName = "WorkHistory", colorHex = 0xFF176B45, type = "INCOME", isDefault = true),
                    CategoryEntity(name = "Pocket Money", iconName = "AccountBalanceWallet", colorHex = 0xFF009688, type = "INCOME", isDefault = true),
                    CategoryEntity(name = "Freelance", iconName = "Laptop", colorHex = 0xFF00BCD4, type = "INCOME", isDefault = true),
                    CategoryEntity(name = "Other Income", iconName = "AttachMoney", colorHex = 0xFF8BC34A, type = "INCOME", isDefault = true)
                )
                categoryDao.insertDefaultCategories(defaults)
            }
        }
    }
}
