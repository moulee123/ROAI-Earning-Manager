package com.roaimoneymanager.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.roaimoneymanager.data.local.AppDatabase
import com.roaimoneymanager.data.network.MfNavClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class NavRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.investmentDao()
            val holdings = dao.getAllHoldingsSync()

            for (holding in holdings) {
                if (!holding.schemeCode.isNullOrBlank()) {
                    val navResult = MfNavClient.fetchLatestNav(holding.schemeCode)
                    if (navResult != null && navResult.nav > 0.0) {
                        val navDateMillis = parseNavDate(navResult.dateString)
                        if (navDateMillis >= holding.lastValuationDateMillis || holding.nav <= 0.0) {
                            val newCurrentValue = (holding.units * navResult.nav * 100.0).toLong()
                            val updated = holding.copy(
                                nav = navResult.nav,
                                currentValuePaise = if (holding.units > 0.0) newCurrentValue else holding.currentValuePaise,
                                lastValuationDateMillis = if (navDateMillis > 0L) navDateMillis else System.currentTimeMillis()
                            )
                            dao.updateHolding(updated)
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun parseNavDate(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
            val date = sdf.parse(dateStr)
            date?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
