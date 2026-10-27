package com.jrustam.ledgerly

import com.jrustam.ledgerly.domain.model.AppLanguage
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.repository.CurrencyPreferences
import com.jrustam.ledgerly.domain.repository.LanguagePreferences
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCurrencyPreferences : CurrencyPreferences {
    override val defaultCurrency = MutableStateFlow(LedgerCurrency.USD)
    override suspend fun setDefaultCurrency(currency: LedgerCurrency) {
        defaultCurrency.value = currency
    }
}

class FakeLanguagePreferences : LanguagePreferences {
    var selected = AppLanguage.SYSTEM
    override fun selectedLanguage() = selected
    override fun setLanguage(language: AppLanguage) {
        selected = language
    }
}
