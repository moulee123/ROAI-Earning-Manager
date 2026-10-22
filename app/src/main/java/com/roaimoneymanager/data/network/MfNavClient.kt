package com.roaimoneymanager.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MfScheme(
    val schemeCode: String,
    val schemeName: String
)

data class MfNavResult(
    val schemeCode: String,
    val schemeName: String,
    val nav: Double,
    val dateString: String
)

object MfNavClient {

    private const val SEARCH_URL = "https://api.mfapi.in/search?q="
    private const val NAV_URL = "https://api.mfapi.in/finance/"

    suspend fun searchSchemes(query: String): List<MfScheme> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<MfScheme>()
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL(SEARCH_URL + encoded)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val jsonArray = JSONArray(responseText)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val code = obj.optString("schemeCode", "")
                        val name = obj.optString("schemeName", "")
                        if (code.isNotBlank() && name.isNotBlank()) {
                            results.add(MfScheme(code, name))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            results
        }
    }

    suspend fun fetchLatestNav(schemeCode: String): MfNavResult? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(NAV_URL + schemeCode)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val root = JSONObject(responseText)
                    val meta = root.optJSONObject("meta")
                    val schemeName = meta?.optString("scheme_name", "Mutual Fund") ?: "Mutual Fund"

                    val dataArray = root.optJSONArray("data")
                    if (dataArray != null && dataArray.length() > 0) {
                        // First item in data array is the latest NAV
                        val latestObj = dataArray.getJSONObject(0)
                        val dateStr = latestObj.optString("date", "")
                        val navStr = latestObj.optString("nav", "0.0")
                        val navVal = navStr.toDoubleOrNull() ?: 0.0

                        return@withContext MfNavResult(
                            schemeCode = schemeCode,
                            schemeName = schemeName,
                            nav = navVal,
                            dateString = dateStr
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }
}
