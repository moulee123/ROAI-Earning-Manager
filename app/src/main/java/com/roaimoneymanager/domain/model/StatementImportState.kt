package com.roaimoneymanager.domain.model

sealed class StatementImportState {
    object Idle : StatementImportState()
    data class Processing(val message: String) : StatementImportState()
    data class Reviewing(
        val candidates: List<CandidateTransaction>,
        val summary: StatementImportSummary
    ) : StatementImportState()
    data class Success(
        val importedCount: Int,
        val totalCreditPaise: Long,
        val totalDebitPaise: Long
    ) : StatementImportState()
    data class Error(val message: String) : StatementImportState()
}
