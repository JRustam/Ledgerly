package com.jrustam.ledgerly.domain.repository

import com.jrustam.ledgerly.domain.model.ExchangeQuote

interface ExchangeRateProvider {
    suspend fun quote(base: String, target: String): ExchangeQuote?
    suspend fun refresh(base: String, target: String): ExchangeQuote
}
