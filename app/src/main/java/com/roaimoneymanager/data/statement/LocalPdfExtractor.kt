package com.roaimoneymanager.data.statement

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object LocalPdfExtractor {

    private @Volatile var isInitialized = false

    private fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    isInitialized = true
                }
            }
        }
    }

    sealed class ExtractionResult {
        data class Success(val text: String, val pageCount: Int) : ExtractionResult()
        data class Error(val message: String) : ExtractionResult()
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri): ExtractionResult {
        return withContext(Dispatchers.IO) {
            ensureInitialized(context)
            var inputStream: InputStream? = null
            var document: PDDocument? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext ExtractionResult.Error("Unable to open the selected PDF file.")

                document = PDDocument.load(inputStream)
                if (document.isEncrypted) {
                    return@withContext ExtractionResult.Error("The PDF statement is password-protected or encrypted. Please remove password protection before importing.")
                }

                val pageCount = document.numberOfPages
                if (pageCount == 0) {
                    return@withContext ExtractionResult.Error("The PDF document contains no pages.")
                }

                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                val text = stripper.getText(document)

                if (text.isBlank()) {
                    return@withContext ExtractionResult.Error("No readable text found. The PDF statement may be scanned or image-based.")
                }

                ExtractionResult.Success(text = text, pageCount = pageCount)
            } catch (e: InvalidPasswordException) {
                ExtractionResult.Error("The PDF statement is password-protected or encrypted.")
            } catch (e: Exception) {
                ExtractionResult.Error("Failed to read PDF document: ${e.localizedMessage ?: "Corrupt or incompatible PDF file."}")
            } finally {
                try {
                    document?.close()
                } catch (_: Exception) {}
                try {
                    inputStream?.close()
                } catch (_: Exception) {}
            }
        }
    }
}
