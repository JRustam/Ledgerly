package com.jrustam.ledgerly.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jrustam.ledgerly.domain.repository.CurrencyPreferences
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class DataStoreCurrencyPreferences @Inject constructor(
    private val store: DataStore<Preferences>,
) : CurrencyPreferences {
    private val key = stringPreferencesKey("default_currency")
    override val defaultCurrency = store.data.map { preferences ->
        LedgerCurrency.entries.firstOrNull { it.name == preferences[key] } ?: LedgerCurrency.USD
    }
    override suspend fun setDefaultCurrency(currency: LedgerCurrency) {
        store.edit { it[key] = currency.name }
    }
}
