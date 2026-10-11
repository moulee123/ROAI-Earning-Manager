package com.roaimoneymanager.data.export

import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils

object CsvExporter {

    fun exportTransactionsToCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        // Header
        sb.append("ID,Date,Time,Title,Type,Category,Amount (INR),Payment Method,Status,Notes\n")

        for (tx in transactions) {
            val dateStr = DateUtils.formatShortDate(tx.dateMillis)
            val timeStr = DateUtils.formatDateTime(tx.dateMillis)
            val amountStr = MoneyUtils.formatPaiseToRupees(tx.amountPaise, showSymbol = false)
            val titleClean = sanitizeCsv(tx.title)
            val categoryClean = sanitizeCsv(tx.category)
            val notesClean = sanitizeCsv(tx.notes)

            sb.append("${tx.id},")
                .append("\"$dateStr\",")
                .append("\"$timeStr\",")
                .append("\"$titleClean\",")
                .append("${tx.type.name},")
                .append("\"$categoryClean\",")
                .append("$amountStr,")
                .append("${tx.paymentMethod.name},")
                .append("${tx.status.name},")
                .append("\"$notesClean\"\n")
        }

        return sb.toString()
    }

    private fun sanitizeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
