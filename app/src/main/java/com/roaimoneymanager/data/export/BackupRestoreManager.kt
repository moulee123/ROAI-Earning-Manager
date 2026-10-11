package com.roaimoneymanager.data.export

import com.roaimoneymanager.data.local.entity.*
import com.roaimoneymanager.data.repository.FinanceRepository
import com.roaimoneymanager.domain.model.DebtStatus
import com.roaimoneymanager.domain.model.DebtType
import com.roaimoneymanager.domain.model.PaymentMethod
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreManager {

    suspend fun createBackupJson(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        workShifts: List<WorkShiftEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        debts: List<DebtEntity>
    ): String {
        val root = JSONObject()
        root.put("appName", "ROAI Money Manager")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Transactions
        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("amountPaise", tx.amountPaise)
            obj.put("title", tx.title)
            obj.put("category", tx.category)
            obj.put("type", tx.type.name)
            obj.put("paymentMethod", tx.paymentMethod.name)
            obj.put("fromAccount", tx.fromAccount ?: "")
            obj.put("toAccount", tx.toAccount ?: "")
            obj.put("dateMillis", tx.dateMillis)
            obj.put("status", tx.status.name)
            obj.put("notes", tx.notes)
            obj.put("isWorkExpense", tx.isWorkExpense)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Categories
        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("iconName", cat.iconName)
            obj.put("colorHex", cat.colorHex)
            obj.put("type", cat.type)
            obj.put("isDefault", cat.isDefault)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Work Shifts
        val workArray = JSONArray()
        for (ws in workShifts) {
            val obj = JSONObject()
            obj.put("id", ws.id)
            obj.put("platform", ws.platform)
            obj.put("dateMillis", ws.dateMillis)
            obj.put("grossEarningsPaise", ws.grossEarningsPaise)
            obj.put("incentivesPaise", ws.incentivesPaise)
            obj.put("hoursWorked", ws.hoursWorked)
            obj.put("expensesPaise", ws.expensesPaise)
            obj.put("notes", ws.notes)
            workArray.put(obj)
        }
        root.put("workShifts", workArray)

        // Savings Goals
        val goalsArray = JSONArray()
        for (g in savingsGoals) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("title", g.title)
            obj.put("targetAmountPaise", g.targetAmountPaise)
            obj.put("currentSavedPaise", g.currentSavedPaise)
            obj.put("targetDateMillis", g.targetDateMillis ?: 0L)
            obj.put("colorHex", g.colorHex)
            obj.put("notes", g.notes)
            goalsArray.put(obj)
        }
        root.put("savingsGoals", goalsArray)

        // Debts
        val debtsArray = JSONArray()
        for (d in debts) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("personName", d.personName)
            obj.put("amountPaise", d.amountPaise)
            obj.put("paidPaise", d.paidPaise)
            obj.put("type", d.type.name)
            obj.put("dateMillis", d.dateMillis)
            obj.put("status", d.status.name)
            obj.put("notes", d.notes)
            debtsArray.put(obj)
        }
        root.put("debts", debtsArray)

        return root.toString(2)
    }

    suspend fun restoreFromJson(jsonString: String, repository: FinanceRepository): Boolean {
        return try {
            val root = JSONObject(jsonString)

            // Restore transactions
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val entity = TransactionEntity(
                        amountPaise = obj.getLong("amountPaise"),
                        title = obj.getString("title"),
                        category = obj.getString("category"),
                        type = TransactionType.valueOf(obj.optString("type", "EXPENSE")),
                        paymentMethod = PaymentMethod.valueOf(obj.optString("paymentMethod", "CASH")),
                        fromAccount = if (obj.has("fromAccount") && !obj.isNull("fromAccount")) obj.getString("fromAccount") else null,
                        toAccount = if (obj.has("toAccount") && !obj.isNull("toAccount")) obj.getString("toAccount") else null,
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        status = TransactionStatus.valueOf(obj.optString("status", "COMPLETED")),
                        notes = obj.optString("notes", ""),
                        isWorkExpense = obj.optBoolean("isWorkExpense", false)
                    )
                    repository.insertTransaction(entity)
                }
            }

            // Restore categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val entity = CategoryEntity(
                        name = obj.getString("name"),
                        iconName = obj.optString("iconName", "Category"),
                        colorHex = obj.optLong("colorHex", 0xFF176B45),
                        type = obj.optString("type", "EXPENSE"),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                    repository.insertCategory(entity)
                }
            }

            // Restore work shifts
            if (root.has("workShifts")) {
                val workArray = root.getJSONArray("workShifts")
                for (i in 0 until workArray.length()) {
                    val obj = workArray.getJSONObject(i)
                    val entity = WorkShiftEntity(
                        platform = obj.getString("platform"),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        grossEarningsPaise = obj.getLong("grossEarningsPaise"),
                        incentivesPaise = obj.optLong("incentivesPaise", 0L),
                        hoursWorked = obj.optDouble("hoursWorked", 0.0),
                        expensesPaise = obj.optLong("expensesPaise", 0L),
                        notes = obj.optString("notes", "")
                    )
                    repository.insertWorkShift(entity)
                }
            }

            // Restore savings goals
            if (root.has("savingsGoals")) {
                val goalsArray = root.getJSONArray("savingsGoals")
                for (i in 0 until goalsArray.length()) {
                    val obj = goalsArray.getJSONObject(i)
                    val entity = SavingsGoalEntity(
                        title = obj.getString("title"),
                        targetAmountPaise = obj.getLong("targetAmountPaise"),
                        currentSavedPaise = obj.optLong("currentSavedPaise", 0L),
                        targetDateMillis = if (obj.optLong("targetDateMillis", 0L) > 0) obj.getLong("targetDateMillis") else null,
                        colorHex = obj.optLong("colorHex", 0xFF176B45),
                        notes = obj.optString("notes", "")
                    )
                    repository.insertSavingsGoal(entity)
                }
            }

            // Restore debts
            if (root.has("debts")) {
                val debtsArray = root.getJSONArray("debts")
                for (i in 0 until debtsArray.length()) {
                    val obj = debtsArray.getJSONObject(i)
                    val entity = DebtEntity(
                        personName = obj.getString("personName"),
                        amountPaise = obj.getLong("amountPaise"),
                        paidPaise = obj.optLong("paidPaise", 0L),
                        type = DebtType.valueOf(obj.optString("type", "LENT")),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        status = DebtStatus.valueOf(obj.optString("status", "PENDING")),
                        notes = obj.optString("notes", "")
                    )
                    repository.insertDebt(entity)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
