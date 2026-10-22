package com.roaimoneymanager.domain.model

enum class DuplicateConfidence {
    LIKELY_DUPLICATE,
    POSSIBLE_DUPLICATE,
    NO_DUPLICATE
}

enum class ExtractionConfidence {
    HIGH,
    MEDIUM,
    LOW_OCR
}

data class CandidateTransaction(
    val id: String,
    val dateMillis: Long,
    val rawDateString: String,
    val description: String, // Editable Display Name
    val originalDescription: String, // Immutable Bank Narration
    val refNumber: String? = null,
    val debitPaise: Long = 0L,
    val creditPaise: Long = 0L,
    val runningBalancePaise: Long? = null,
    val category: String = "General",
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val notes: String = "", // Editable Personal Reason / Notes
    val isSelected: Boolean = true,
    val duplicateConfidence: DuplicateConfidence = DuplicateConfidence.NO_DUPLICATE,
    val duplicateReason: String? = null,
    val needsReview: Boolean = false,
    val confidence: ExtractionConfidence = ExtractionConfidence.HIGH
) {
    val isIncome: Boolean
        get() = creditPaise > 0L && debitPaise == 0L

    val amountPaise: Long
        get() = if (creditPaise > 0L) creditPaise else debitPaise
}

data class StatementImportSummary(
    val bankName: String = "Canara Bank",
    val fileName: String = "",
    val totalCandidates: Int = 0,
    val totalSelected: Int = 0,
    val totalCreditPaise: Long = 0L,
    val totalDebitPaise: Long = 0L,
    val likelyDuplicateCount: Int = 0,
    val possibleDuplicateCount: Int = 0,
    val needsReviewCount: Int = 0,
    val reconciliationStatus: ReconciliationStatus = ReconciliationStatus.UNAVAILABLE
)

enum class ReconciliationStatus {
    CONSISTENT,
    NEEDS_REVIEW,
    UNAVAILABLE
}
