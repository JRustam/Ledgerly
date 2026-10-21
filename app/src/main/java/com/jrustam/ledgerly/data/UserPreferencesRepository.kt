package com.jrustam.ledgerly.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jrustam.ledgerly.domain.*
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

/** AppCompat persists locales on API 24–32; the framework persists them on API 33+. */
class AppCompatLanguagePreferences @Inject constructor() : LanguagePreferences {
    override fun selectedLanguage(): AppLanguage =
        AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(','))
    override fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
