package com.roaimoneymanager.data.statement

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.roaimoneymanager.domain.model.CandidateTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream

object XlsxStatementParser {

    suspend fun parseXlsxUri(context: Context, uri: Uri): List<CandidateTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
                val sharedStrings = mutableListOf<String>()
                val rows = mutableListOf<List<String>>()

                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name.endsWith("sharedStrings.xml") -> {
                                sharedStrings.addAll(parseSharedStrings(zis))
                            }
                            entry.name.contains("worksheets/sheet") -> {
                                rows.addAll(parseWorksheet(zis, sharedStrings))
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                CsvStatementParser.parseCsvText(
                    rows.map { row -> row.joinToString(",") { escapeCsv(it) } }.joinToString("\n"),
                    "Excel Bank Statement"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun parseSharedStrings(zis: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(zis, Charsets.UTF_8.name())
        var eventType = parser.eventType
        val currentText = StringBuilder()
        var isReadingText = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        isReadingText = true
                        currentText.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (isReadingText) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        isReadingText = false
                        strings.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseWorksheet(zis: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(zis, Charsets.UTF_8.name())
        var eventType = parser.eventType
        var currentRow = mutableListOf<String>()
        var currentCellType = ""
        val currentCellVal = StringBuilder()
        var isReadingVal = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentCellVal.setLength(0)
                        }
                        "v" -> isReadingVal = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (isReadingVal) {
                        currentCellVal.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> isReadingVal = false
                        "c" -> {
                            val rawVal = currentCellVal.toString()
                            val cellValue = if (currentCellType == "s" && rawVal.toIntOrNull() != null) {
                                sharedStrings.getOrElse(rawVal.toInt()) { rawVal }
                            } else {
                                parseExcelCellNumber(rawVal, currentCellType)
                            }
                            currentRow.add(cellValue)
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                rows.add(currentRow)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun parseExcelCellNumber(value: String, type: String): String {
        if (type == "s") return value
        val d = value.toDoubleOrNull() ?: return value
        if (d > 30000.0 && d < 60000.0) {
            try {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.set(1899, Calendar.DECEMBER, 30)
                calendar.add(Calendar.DAY_OF_YEAR, d.toInt())
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                return sdf.format(calendar.time)
            } catch (_: Exception) {}
        }
        return value
    }

    private fun escapeCsv(text: String): String {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }
}
