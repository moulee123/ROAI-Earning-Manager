package com.roaimoneymanager.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

enum class PaymentMethod {
    CASH,
    UPI,
    BANK
}

enum class TransactionStatus {
    COMPLETED,
    PENDING
}

enum class DebtType {
    LENT,       // Money I gave to someone (they owe me)
    BORROWED    // Money I took from someone (I owe them)
}

enum class DebtStatus {
    PENDING,
    SETTLED
}

enum class ReportTimeFrame {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
