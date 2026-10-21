package com.jrustam.ledgerly.domain

import kotlinx.coroutines.flow.Flow

enum class LedgerCurrency { AZN, USD, EUR, RUB }

enum class AppLanguage(val tag: String) {
    SYSTEM(""), AZERBAIJANI("az"), ENGLISH("en"), RUSSIAN("ru"), TURKISH("tr");
    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag.substringBefore('-') } ?: SYSTEM
    }
}

interface CurrencyPreferences {
    val defaultCurrency: Flow<LedgerCurrency>
    suspend fun setDefaultCurrency(currency: LedgerCurrency)
}

interface LanguagePreferences {
    fun selectedLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
}
