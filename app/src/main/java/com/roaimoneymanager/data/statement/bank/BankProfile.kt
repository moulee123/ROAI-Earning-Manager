package com.roaimoneymanager.data.statement.bank

import com.roaimoneymanager.domain.model.CandidateTransaction

interface BankProfile {
    val bankName: String
    val supportedHeaderKeywords: List<String>
    
    fun parseRows(rows: List<List<String>>, fileName: String): List<CandidateTransaction>
}
