package com.roaimoneymanager.data.statement

import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.domain.model.CandidateTransaction
import com.roaimoneymanager.domain.model.DuplicateConfidence
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils
import java.util.Locale

object DuplicateDetectionService {

    fun analyzeDuplicates(
        candidates: List<CandidateTransaction>,
        existingTransactions: List<TransactionEntity>
    ): List<CandidateTransaction> {
        val processed = mutableListOf<CandidateTransaction>()

        for (candidate in candidates) {
            val candidateIsIncome = candidate.isIncome
            val candidateAmount = candidate.amountPaise
            val candidateDateStr = DateUtils.formatDate(candidate.dateMillis)

            // 1. Check for match with existing Room transactions
            var confidence = DuplicateConfidence.NO_DUPLICATE
            var reason: String? = null

            // Ref number match
            val candidateRef = candidate.refNumber
            if (!candidateRef.isNullOrBlank()) {
                val refMatch = existingTransactions.find { tx ->
                    tx.amountPaise == candidateAmount &&
                            (tx.title.contains(candidateRef, ignoreCase = true) ||
                                    tx.notes.contains(candidateRef, ignoreCase = true))
                }
                if (refMatch != null) {
                    confidence = DuplicateConfidence.LIKELY_DUPLICATE
                    reason = "Matches existing reference #${candidate.refNumber}"
                }
            }

            // Exact date and amount match
            if (confidence == DuplicateConfidence.NO_DUPLICATE) {
                val dayStart = DateUtils.getStartOfDay(candidate.dateMillis)
                val dayEnd = DateUtils.getEndOfDay(candidate.dateMillis)

                val sameDayTxs = existingTransactions.filter { tx ->
                    tx.dateMillis in dayStart..dayEnd && tx.amountPaise == candidateAmount
                }

                if (sameDayTxs.isNotEmpty()) {
                    val candidateType = if (candidateIsIncome) TransactionType.INCOME else TransactionType.EXPENSE
                    val directionMatch = sameDayTxs.find { it.type == candidateType }

                    if (directionMatch != null) {
                        val normCand = normalizeDesc(candidate.description)
                        val normEx = normalizeDesc(directionMatch.title)

                        if (normCand.contains(normEx) || normEx.contains(normCand) || calculateSimilarity(normCand, normEx) > 0.7) {
                            confidence = DuplicateConfidence.LIKELY_DUPLICATE
                            reason = "Likely duplicate of ${directionMatch.title} ($candidateDateStr)"
                        } else {
                            confidence = DuplicateConfidence.POSSIBLE_DUPLICATE
                            reason = "Possible duplicate: same date & amount on $candidateDateStr"
                        }
                    }
                }
            }

            // 2. Check for duplicate row within the current import batch
            if (confidence == DuplicateConfidence.NO_DUPLICATE) {
                val batchDuplicate = processed.find { prev ->
                    prev.dateMillis == candidate.dateMillis &&
                            prev.amountPaise == candidateAmount &&
                            prev.isIncome == candidateIsIncome &&
                            normalizeDesc(prev.description) == normalizeDesc(candidate.description)
                }

                if (batchDuplicate != null) {
                    confidence = DuplicateConfidence.LIKELY_DUPLICATE
                    reason = "Duplicate transaction within this statement"
                }
            }

            // Deselect likely duplicates by default, leave possible duplicates selected
            val shouldSelect = candidate.isSelected && (confidence != DuplicateConfidence.LIKELY_DUPLICATE)

            processed.add(
                candidate.copy(
                    duplicateConfidence = confidence,
                    duplicateReason = reason,
                    isSelected = shouldSelect
                )
            )
        }

        return processed
    }

    private fun CharSequence?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    private fun normalizeDesc(desc: String): String {
        return desc.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = maxOf(s1.length, s2.length)
        var common = 0
        for (i in 0 until minOf(s1.length, s2.length)) {
            if (s1[i] == s2[i]) common++
        }
        return common.toDouble() / maxLen.toDouble()
    }
}
