package com.roaimoneymanager.data.statement

import android.content.Context
import android.net.Uri
import com.roaimoneymanager.domain.model.CandidateTransaction
import com.roaimoneymanager.domain.model.ExtractionConfidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

object UniversalStatementParser {

    data class ParseResult(
        val bankName: String,
        val candidates: List<CandidateTransaction>,
        val errorMessage: String? = null
    )

    suspend fun parseStatement(context: Context, uri: Uri, fileName: String): ParseResult {
        return withContext(Dispatchers.IO) {
            val lowerName = fileName.lowercase(Locale.ROOT)
            val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT) ?: ""

            when {
                lowerName.endsWith(".csv") || mimeType.contains("csv") || mimeType.contains("comma-separated") -> {
                    val csvText = readTextFromUri(context, uri)
                    val candidates = CsvStatementParser.parseCsvText(csvText, "CSV Statement")
                    if (candidates.isEmpty()) {
                        ParseResult("CSV Account", emptyList(), "No valid transactions found in the CSV file.")
                    } else {
                        ParseResult("CSV Account", candidates)
                    }
                }

                lowerName.endsWith(".xlsx") || mimeType.contains("spreadsheetml") -> {
                    val candidates = XlsxStatementParser.parseXlsxUri(context, uri)
                    if (candidates.isEmpty()) {
                        ParseResult("Excel Account", emptyList(), "No valid transactions found in the Excel file.")
                    } else {
                        ParseResult("Excel Account", candidates)
                    }
                }

                lowerName.endsWith(".pdf") || mimeType.contains("pdf") -> {
                    val extractionResult = LocalPdfExtractor.extractTextFromUri(context, uri)
                    when (extractionResult) {
                        is LocalPdfExtractor.ExtractionResult.Success -> {
                            val text = extractionResult.text
                            val candidates = CanaraStatementParser.parseStatementText(text)
                            if (candidates.isNotEmpty()) {
                                ParseResult("Canara Bank", candidates)
                            } else {
                                val genericCandidates = CsvStatementParser.parseCsvText(text, "PDF Statement")
                                if (genericCandidates.isNotEmpty()) {
                                    ParseResult("PDF Statement", genericCandidates)
                                } else {
                                    ParseResult("PDF Statement", emptyList(), "Could not parse transactions from PDF text. The layout might be unsupported.")
                                }
                            }
                        }
                        is LocalPdfExtractor.ExtractionResult.Error -> {
                            val ocrText = OcrStatementExtractor.extractTextFromImageUri(context, uri)
                            if (ocrText.isNotBlank()) {
                                val candidates = CsvStatementParser.parseCsvText(ocrText, "Scanned Statement")
                                val marked = candidates.map { it.copy(confidence = ExtractionConfidence.LOW_OCR) }
                                ParseResult("Scanned Statement (OCR)", marked)
                            } else {
                                ParseResult("PDF Statement", emptyList(), extractionResult.message)
                            }
                        }
                    }
                }

                lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || mimeType.contains("image") -> {
                    val ocrText = OcrStatementExtractor.extractTextFromImageUri(context, uri)
                    if (ocrText.isBlank()) {
                        ParseResult("Image Statement", emptyList(), "No readable text extracted from the image via OCR.")
                    } else {
                        val candidates = CsvStatementParser.parseCsvText(ocrText, "Image Statement")
                        val marked = candidates.map { it.copy(confidence = ExtractionConfidence.LOW_OCR) }
                        ParseResult("Image Statement (OCR)", marked)
                    }
                }

                else -> {
                    val text = readTextFromUri(context, uri)
                    val candidates = CsvStatementParser.parseCsvText(text, "Generic Statement")
                    if (candidates.isEmpty()) {
                        ParseResult("Unknown", emptyList(), "Unsupported file format or unreadable document.")
                    } else {
                        ParseResult("Generic Statement", candidates)
                    }
                }
            }
        }
    }

    private fun readTextFromUri(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sb.toString()
    }
}
