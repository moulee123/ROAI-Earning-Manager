package com.roaimoneymanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class OpeningBalances(
    val cashPaise: Long = 0L,
    val bankPaise: Long = 0L,
    val upiPaise: Long = 0L
) {
    val totalOpeningPaise: Long
        get() = cashPaise + bankPaise + upiPaise
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val OPENING_CASH_PAISE = longPreferencesKey("opening_cash_paise")
        val OPENING_BANK_PAISE = longPreferencesKey("opening_bank_paise")
        val OPENING_UPI_PAISE = longPreferencesKey("opening_upi_paise")
        val IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
    }

    val openingBalancesFlow: Flow<OpeningBalances> = context.dataStore.data.map { preferences ->
        OpeningBalances(
            cashPaise = preferences[PreferencesKeys.OPENING_CASH_PAISE] ?: 0L,
            bankPaise = preferences[PreferencesKeys.OPENING_BANK_PAISE] ?: 0L,
            upiPaise = preferences[PreferencesKeys.OPENING_UPI_PAISE] ?: 0L
        )
    }

    val isSetupCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_SETUP_COMPLETED] ?: false
    }

    suspend fun updateOpeningBalances(cashPaise: Long, bankPaise: Long, upiPaise: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENING_CASH_PAISE] = cashPaise
            preferences[PreferencesKeys.OPENING_BANK_PAISE] = bankPaise
            preferences[PreferencesKeys.OPENING_UPI_PAISE] = upiPaise
            preferences[PreferencesKeys.IS_SETUP_COMPLETED] = true
        }
    }
}
