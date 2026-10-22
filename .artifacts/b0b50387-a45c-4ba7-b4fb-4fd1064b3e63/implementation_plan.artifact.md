# Universal Bank Statement Import Feature

This plan outlines the architecture and implementation steps to introduce a Universal Bank Statement Import feature to ROAI Money Manager, supporting multiple banks and formats (PDF, CSV, XLSX, and OCR for scanned PDFs) while strictly maintaining privacy, local-first processing, and financial data integrity.

## User Review Required

- **XLSX Parsing Strategy:** To avoid bloating the app with heavy Apache POI dependencies, I will implement a lightweight, zero-dependency `SimpleXlsxParser` that reads XLSX files as zipped XML (using Android's built-in `ZipFile` and `XmlPullParser`). This ensures maximum compatibility and performance.
- **Database Schema Migration:** To cleanly preserve original bank descriptions without polluting user notes, I propose adding two optional columns (`originalDescription` and `bankReference`) to `TransactionEntity`. The database version will increment to 3 with a simple migration.
- **OCR Library:** I will integrate Google's **ML Kit Text Recognition** (`com.google.android.gms:play-services-mlkit-text-recognition`) for on-device OCR fallback. It is fully local, requires no network calls, and aligns with the privacy requirements.

## Proposed Changes

### Dependency & Build Configurations

#### [MODIFY] app/build.gradle.kts
- Add ML Kit Text Recognition dependency for OCR fallback: `implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")`.
- Add AndroidX DocumentFile for better SAF file handling (optional but recommended): `implementation("androidx.documentfile:documentfile:1.0.1")`.

### Core Architecture & Database

#### [MODIFY] app/src/main/java/com/roaimoneymanager/data/local/entity/TransactionEntity.kt
- Add `originalDescription: String? = null`.
- Add `bankReference: String? = null`.

#### [MODIFY] app/src/main/java/com/roaimoneymanager/data/local/AppDatabase.kt
- Increment version to 3.
- Provide a database migration from v2 to v3 adding the new columns.

#### [MODIFY] app/src/main/java/com/roaimoneymanager/domain/model/CandidateTransaction.kt
- Add `originalDescription: String`.
- Make `description` the user-editable "Display Name".
- Add `notes: String` for user's personal reason.
- Add `confidence: ExtractionConfidence` (High, Medium, Low-OCR).

### Statement Parsers & Extraction

#### [NEW] app/src/main/java/com/roaimoneymanager/data/statement/UniversalStatementParser.kt
- The coordinator that detects file type (PDF, CSV, XLSX, Image/Scanned PDF) by reading the file's header bytes or MIME type.
- Delegates to the respective parser based on detection.

#### [NEW] app/src/main/java/com/roaimoneymanager/data/statement/CsvStatementParser.kt
- Robust CSV parsing supporting quoted fields, comma escaping, multiple date formats, and header detection.
- Normalizes rows into `CandidateTransaction`.

#### [NEW] app/src/main/java/com/roaimoneymanager/data/statement/XlsxStatementParser.kt
- Uses `ZipInputStream` and `XmlPullParser` to extract strings from `sharedStrings.xml` and rows/cells from `sheet1.xml`.
- Normalizes rows into `CandidateTransaction`.

#### [NEW] app/src/main/java/com/roaimoneymanager/data/statement/OcrStatementExtractor.kt
- Takes an Image URI or Scanned PDF (rendered to bitmap), uses ML Kit to extract text blocks.
- Marks transactions with `ExtractionConfidence.LOW_OCR` requiring user review.

#### [NEW] app/src/main/java/com/roaimoneymanager/data/statement/bank/BankProfile.kt
- Interface for specific bank rules (e.g., date formats, known headers, amount columns).
- Concrete implementations: `CanaraBankProfile`, `GenericBankProfile` (fallback).

### UI and ViewModel

#### [MODIFY] app/src/main/java/com/roaimoneymanager/ui/viewmodel/MainViewModel.kt
- Update `processBankStatementPdf` to become `processBankStatementFile(uri, mimeType)`.
- Coordinate the extraction, parsing, duplicate detection, and review setup.
- Handle user edits to `description` (display name) and `notes` (reason).
- Save transactions to the repository upon explicit confirmation.

#### [MODIFY] app/src/main/java/com/roaimoneymanager/ui/screens/StatementImportReviewScreen.kt
- Update `CandidateRowItem` to support inline editing of the "Display Name" and "Reason/Notes".
- Expand the details section to show the `originalDescription` and `bankReference`.
- Show OCR low-confidence warnings.

#### [MODIFY] app/src/main/java/com/roaimoneymanager/ui/screens/TransactionsScreen.kt
- Add an "Import Statement" floating action button or top app bar action.
- Wire it to an `ActivityResultContracts.OpenDocument` launcher accepting `application/pdf`, `text/csv`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

## Verification Plan

### Automated Tests
- Provide unit tests in `app/src/test/` for `CsvStatementParser` and `XlsxStatementParser` using mocked or local data.
- Ensure `DuplicateDetectionService` accurately identifies duplicates without false positives.
- Test date parsing, number parsing, and debit/credit assignments.

### Manual Verification
- Deploy to an emulator and test importing sample CSVs and PDFs.
- Verify user edits in the Review screen are retained after confirmation and visible in the Dashboard/Transactions list.
- Confirm that database migrations do not crash existing data.
