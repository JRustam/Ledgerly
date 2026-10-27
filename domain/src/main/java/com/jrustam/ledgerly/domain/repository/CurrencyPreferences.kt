package com.jrustam.ledgerly.domain.repository

import com.jrustam.ledgerly.domain.model.LedgerCurrency
import kotlinx.coroutines.flow.Flow

interface CurrencyPreferences {
    val defaultCurrency: Flow<LedgerCurrency>
    suspend fun setDefaultCurrency(currency: LedgerCurrency)
}
