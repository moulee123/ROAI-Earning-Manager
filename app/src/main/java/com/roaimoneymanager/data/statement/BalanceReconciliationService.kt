package com.roaimoneymanager.data.statement

import com.roaimoneymanager.domain.model.CandidateTransaction
import com.roaimoneymanager.domain.model.ReconciliationStatus

object BalanceReconciliationService {

    fun checkReconciliation(candidates: List<CandidateTransaction>): ReconciliationStatus {
        val rowsWithBalance = candidates.filter { it.runningBalancePaise != null }
        if (rowsWithBalance.size < 2) {
            return ReconciliationStatus.UNAVAILABLE
        }

        var isChronologicalConsistent = true
        var isReverseConsistent = true

        // Test chronological order
        for (i in 0 until rowsWithBalance.size - 1) {
            val curr = rowsWithBalance[i]
            val next = rowsWithBalance[i + 1]

            val expectedNextBalance = curr.runningBalancePaise!! + next.creditPaise - next.debitPaise
            if (expectedNextBalance != next.runningBalancePaise) {
                isChronologicalConsistent = false
                break
            }
        }

        if (isChronologicalConsistent) {
            return ReconciliationStatus.CONSISTENT
        }

        // Test reverse chronological order
        for (i in 0 until rowsWithBalance.size - 1) {
            val curr = rowsWithBalance[i]
            val next = rowsWithBalance[i + 1]

            // In reverse order, next is older than curr
            val expectedPrevBalance = curr.runningBalancePaise!! - curr.creditPaise + curr.debitPaise
            if (expectedPrevBalance != next.runningBalancePaise) {
                isReverseConsistent = false
                break
            }
        }

        return if (isReverseConsistent) ReconciliationStatus.CONSISTENT else ReconciliationStatus.NEEDS_REVIEW
    }
}
