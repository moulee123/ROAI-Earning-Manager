package com.roaimoneymanager.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object MoneyUtils {

    private val decimalFormat = DecimalFormat("##,##,##0.00")

    /**
     * Converts paise (Long) to Rupees String formatted with ₹ symbol.
     * e.g., 10050L -> "₹100.50", 12867L -> "₹128.67"
     */
    fun formatPaiseToRupees(paise: Long, showSymbol: Boolean = true): String {
        val bd = BigDecimal.valueOf(paise).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        val formatted = decimalFormat.format(bd)
        return if (showSymbol) "₹$formatted" else formatted
    }

    /**
     * Converts paise (Long) to Rupees BigDecimal.
     */
    fun paiseToRupeesBigDecimal(paise: Long): BigDecimal {
        return BigDecimal.valueOf(paise).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    }

    /**
     * Converts rupee input string (e.g. "128.67", "0.01", "100.99", "₹1,000") to Long paise safely.
     * Uses BigDecimal to avoid floating point precision errors (e.g. 128.67 * 100 becoming 12866.999999...).
     * Returns 0L if invalid or empty.
     */
    fun parseRupeesToPaise(input: String): Long {
        if (input.isBlank()) return 0L
        val cleanInput = input.replace("₹", "")
            .replace(",", "")
            .trim()

        return try {
            val bd = BigDecimal(cleanInput)
            bd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Validates whether a given text string is a valid positive money amount.
     */
    fun isValidAmount(input: String): Boolean {
        if (input.isBlank()) return false
        val clean = input.replace("₹", "").replace(",", "").trim()
        return try {
            val bd = BigDecimal(clean)
            bd > BigDecimal.ZERO
        } catch (e: Exception) {
            false
        }
    }
}
