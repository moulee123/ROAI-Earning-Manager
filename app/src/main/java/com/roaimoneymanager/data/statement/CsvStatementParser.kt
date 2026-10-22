package com.roaimoneymanager.data.statement

import com.roaimoneymanager.domain.model.CandidateTransaction
import com.roaimoneymanager.domain.model.ExtractionConfidence
import com.roaimoneymanager.domain.model.PaymentMethod
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object CsvStatementParser {

    private val AMOUNT_REGEX = Pattern.compile("[-+]?\\b\\d{1,3}(?:,\\d{2,3})*\\.\\d{2}\\b")

    fun parseCsvText(csvText: String, bankName: String = "Generic Bank"): List<CandidateTransaction> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val rows = lines.map { parseCsvLine(it) }
        if (rows.isEmpty()) return emptyList()

        // Find header row or assume first row is header
        var headerIndex = 0
        var dateCol = -1
        var descCol = -1
        var debitCol = -1
        var creditCol = -1
        var amountCol = -1
        var refCol = -1

        for ((index, row) in rows.withIndex()) {
            val lowerRow = row.map { it.lowercase(Locale.ROOT) }
            val dIdx = lowerRow.indexOfFirst { it.contains("date") || it.contains("txn date") || it.contains("transaction date") }
            val descIdx = lowerRow.indexOfFirst { it.contains("narration") || it.contains("description") || it.contains("particulars") || it.contains("details") }
            
            if (dIdx != -1 && descIdx != -1) {
                headerIndex = index
                dateCol = dIdx
                descCol = descIdx
                debitCol = lowerRow.indexOfFirst { it.contains("debit") || it.contains("withdrawal") || it.contains("dr") }
                creditCol = lowerRow.indexOfFirst { it.contains("credit") || it.contains("deposit") || it.contains("cr") }
                amountCol = lowerRow.indexOfFirst { (it.contains("amount") || it.contains("value")) && debitCol == -1 && creditCol == -1 }
                refCol = lowerRow.indexOfFirst { it.contains("ref") || it.contains("utr") || it.contains("cheque") || it.contains("txn id") }
                break
            }
        }

        // If no explicit header found, fallback to standard column assumptions (Date=0, Desc=1, Amount=2)
        if (dateCol == -1) dateCol = 0
        if (descCol == -1) descCol = 1

        val candidates = mutableListOf<CandidateTransaction>()
        val dataRows = if (headerIndex >= 0 && headerIndex < rows.size) rows.subList(headerIndex + 1, rows.size) else rows

        for ((idx, row) in dataRows.withIndex()) {
            if (row.size <= maxOf(dateCol, descCol)) continue

            val rawDate = row.getOrElse(dateCol) { "" }
            val dateMillis = parseDateToMillis(rawDate) ?: continue

            val rawDesc = row.getOrElse(descCol) { "Bank Transaction" }
            if (rawDesc.isBlank()) continue

            var debitPaise = 0L
            var creditPaise = 0L

            if (debitCol != -1 && row.size > debitCol) {
                debitPaise = parseAmountToPaise(row[debitCol])
            }
            if (creditCol != -1 && row.size > creditCol) {
                creditPaise = parseAmountToPaise(row[creditCol])
            }

            // If separate debit/credit not found, look at amountCol or scan all columns for amounts
            if (debitPaise == 0L && creditPaise == 0L) {
                if (amountCol != -1 && row.size > amountCol) {
                    val amtStr = row[amountCol]
                    val paise = parseAmountToPaise(amtStr)
                    if (amtStr.contains("-") || rawDesc.uppercase(Locale.ROOT).contains("DR")) {
                        debitPaise = paise
                    } else {
                        creditPaise = paise
                    }
                } else {
                    // Scan row for any monetary value
                    for (cell in row) {
                        val matcher = AMOUNT_REGEX.matcher(cell)
                        if (matcher.find()) {
                            val paise = parseAmountToPaise(matcher.group() ?: "0.00")
                            if (cell.startsWith("-") || cell.uppercase(Locale.ROOT).contains("DR")) {
                                debitPaise = paise
                            } else {
                                creditPaise = paise
                            }
                            break
                        }
                    }
                }
            }

            if (debitPaise == 0L && creditPaise == 0L) continue

            val refNum = if (refCol != -1 && row.size > refCol) row[refCol].ifBlank { null } else null
            val paymentMethod = PaymentMethod.UPI

            val category = autoDetectCategory(rawDesc)

            candidates.add(
                CandidateTransaction(
                    id = "csv_import_${idx}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                    dateMillis = dateMillis,
                    rawDateString = rawDate,
                    description = rawDesc,
                    originalDescription = rawDesc,
                    refNumber = refNum,
                    debitPaise = debitPaise,
                    creditPaise = creditPaise,
                    category = category,
                    paymentMethod = paymentMethod,
                    confidence = ExtractionConfidence.HIGH
                )
            )
        }

        return candidates
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        // skip next
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun parseAmountToPaise(amountStr: String): Long {
        return try {
            val clean = amountStr.replace(",", "").replace("₹", "").replace("Rs.", "").trim()
            val bd = BigDecimal(clean.replace("+", ""))
            bd.multiply(BigDecimal("100")).abs().toLong()
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        val clean = dateStr.trim().replace("-", "/").replace(".", "/")
        val patterns = listOf(
            "dd/MM/yyyy",
            "yyyy/MM/dd",
            "d/M/yyyy",
            "dd/MMM/yyyy",
            "dd/MM/yy"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(clean)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun autoDetectCategory(description: String): String {
        val lower = description.lowercase(Locale.ROOT)
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("restaurant") -> "Food"
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("hpcl") || lower.contains("bpcl") -> "Petrol"
            lower.contains("uber") || lower.contains("ola") || lower.contains("transport") || lower.contains("metro") -> "Transport"
            lower.contains("college") || lower.contains("school") || lower.contains("fees") -> "College"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("shopping") -> "Shopping"
            lower.contains("recharge") || lower.contains("bill") || lower.contains("electricity") -> "Bills"
            lower.contains("salary") -> "Salary"
            lower.contains("freelance") -> "Freelance"
            else -> "General"
        }
    }
}
