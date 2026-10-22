package com.roaimoneymanager.data.statement

import com.roaimoneymanager.domain.model.CandidateTransaction
import com.roaimoneymanager.domain.model.ExtractionConfidence
import com.roaimoneymanager.domain.model.PaymentMethod
import com.roaimoneymanager.util.MoneyUtils
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object CanaraStatementParser {

    // Regex for date matching e.g. 11/10/2026, 11-10-2026, 11-OCT-2026, 11/10/26
    private val DATE_REGEX = Pattern.compile("^(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}|\\d{1,2}[/\\-][A-Za-z]{3}[/\\-]\\d{2,4})")

    // Regex for Indian currency amounts e.g. 128.67, 1,250.00, 1,50,000.00
    private val AMOUNT_REGEX = Pattern.compile("(?:₹|Rs\\.?\\s*)?\\b(\\d{1,3}(?:,\\d{2,3})*\\.\\d{2})\\b")

    // Ref number regex e.g. UPI/123456789012, UTR/123456, Ref: 12345
    private val REF_REGEX = Pattern.compile("\\b(\\d{10,12}|UPI/\\w+/\\d{10,12}|NEFT[\\w*\\-]+|IMPS[\\w*\\-]+)\\b", Pattern.CASE_INSENSITIVE)

    private val IGNORED_KEYWORDS = listOf(
        "statement of account", "canara bank", "opening balance", "closing balance",
        "total debit", "total credit", "page ", "particulars", "withdrawal", "deposit",
        "balance", "txn date", "value date", "chq no", "account number", "ifsc code"
    )

    fun parseStatementText(extractedText: String): List<CandidateTransaction> {
        val lines = extractedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidates = mutableListOf<CandidateTransaction>()

        var currentCandidate: RawCandidate? = null

        for (line in lines) {
            val lowerLine = line.lowercase(Locale.ROOT)

            // Skip page headers, footers, summary lines
            if (IGNORED_KEYWORDS.any { lowerLine.contains(it) }) {
                continue
            }

            val dateMatcher = DATE_REGEX.matcher(line)
            if (dateMatcher.find()) {
                // If we already have a candidate in progress, flush it
                currentCandidate?.let { raw ->
                    buildCandidateTransaction(raw, candidates.size + 1)?.let { candidates.add(it) }
                }

                val dateStr = dateMatcher.group(1) ?: ""
                val restOfLine = line.substring(dateMatcher.end()).trim()

                currentCandidate = RawCandidate(
                    rawDateStr = dateStr,
                    lines = mutableListOf(restOfLine)
                )
            } else {
                // Continuation line for multiline description
                currentCandidate?.lines?.add(line)
            }
        }

        // Flush last candidate
        currentCandidate?.let { raw ->
            buildCandidateTransaction(raw, candidates.size + 1)?.let { candidates.add(it) }
        }

        return candidates
    }

    private data class RawCandidate(
        val rawDateStr: String,
        val lines: MutableList<String>
    )

    private fun buildCandidateTransaction(raw: RawCandidate, index: Int): CandidateTransaction? {
        val fullText = raw.lines.joinToString(" ").trim()
        if (fullText.isBlank()) return null

        val parsedDateMillis = parseDateToMillis(raw.rawDateStr) ?: return null

        // Extract all currency amounts in fullText
        val amountMatcher = AMOUNT_REGEX.matcher(fullText)
        val amounts = mutableListOf<Long>()
        while (amountMatcher.find()) {
            val amtGroup = amountMatcher.group(1) ?: continue
            val paise = parseAmountToPaise(amtGroup)
            if (paise > 0L) {
                amounts.add(paise)
            }
        }

        if (amounts.isEmpty()) {
            return null // No numeric amount found
        }

        // In a typical statement row:
        // Case A: 2 amounts -> [Transaction Amount, Running Balance]
        // Case B: 3 amounts -> [Debit/Credit Amount, Other Amount, Running Balance]
        // Case C: 1 amount -> [Transaction Amount]
        val (transactionAmountPaise, runningBalancePaise) = when (amounts.size) {
            1 -> amounts[0] to null
            2 -> amounts[0] to amounts[1]
            else -> amounts[0] to amounts.last()
        }

        val upperText = fullText.uppercase(Locale.ROOT)

        // Determine if Debit (Expense) or Credit (Income)
        val isCredit = upperText.contains("BY TRANSFER") ||
                upperText.contains("CR/") ||
                upperText.contains("CREDIT") ||
                upperText.contains("DEPOSIT") ||
                upperText.contains("SALARY") ||
                upperText.contains("INT PAID") ||
                upperText.contains("REFUND")

        val debitPaise = if (isCredit) 0L else transactionAmountPaise
        val creditPaise = if (isCredit) transactionAmountPaise else 0L

        // Extract Ref Number if available
        val refMatcher = REF_REGEX.matcher(fullText)
        val refNumber = if (refMatcher.find()) refMatcher.group(1) else null

        // Clean description (remove raw amounts from description text)
        var cleanDescription = fullText
            .replace(AMOUNT_REGEX.pattern().toRegex(), "")
            .replace(DATE_REGEX.pattern().toRegex(), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanDescription.isBlank()) {
            cleanDescription = if (isCredit) "Bank Deposit" else "Bank Withdrawal"
        }

        // Determine Category
        val category = autoDetectCategory(cleanDescription)

        // Determine Payment Method
        val paymentMethod = PaymentMethod.UPI

        return CandidateTransaction(
            id = "canara_import_" + index + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(6),
            dateMillis = parsedDateMillis,
            rawDateString = raw.rawDateStr,
            description = cleanDescription,
            originalDescription = cleanDescription,
            refNumber = refNumber,
            debitPaise = debitPaise,
            creditPaise = creditPaise,
            runningBalancePaise = runningBalancePaise,
            category = category,
            paymentMethod = paymentMethod,
            isSelected = true,
            confidence = ExtractionConfidence.HIGH
        )
    }

    private fun autoDetectCategory(description: String): String {
        val lower = description.lowercase(Locale.ROOT)
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("restaurant") -> "Food"
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("hpcl") || lower.contains("bpcl") || lower.contains("iocl") -> "Petrol"
            lower.contains("uber") || lower.contains("ola") || lower.contains("bus") || lower.contains("metro") || lower.contains("irctc") -> "Transport"
            lower.contains("college") || lower.contains("school") || lower.contains("fees") || lower.contains("university") -> "College"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("shopping") -> "Shopping"
            lower.contains("recharge") || lower.contains("airtel") || lower.contains("jio") || lower.contains("electricity") || lower.contains("bill") -> "Bills"
            lower.contains("salary") -> "Salary"
            lower.contains("freelance") -> "Freelance"
            lower.contains("work") -> "Work Income"
            else -> "General"
        }
    }

    private fun parseAmountToPaise(amountStr: String): Long {
        return try {
            val clean = amountStr.replace(",", "").replace("₹", "").replace("Rs.", "").trim()
            val bd = BigDecimal(clean)
            bd.multiply(BigDecimal("100")).toLong()
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        val clean = dateStr.trim().replace("-", "/").replace(".", "/")
        val patterns = listOf(
            "dd/MM/yyyy",
            "d/M/yyyy",
            "dd/MMM/yyyy",
            "dd/MM/yy",
            "d/M/yy"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(clean)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {}
        }
        return null
    }
}
