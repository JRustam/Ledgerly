package com.jrustam.ledgerly.domain.model

import java.math.BigInteger

sealed interface CurrencyConversion {
    data object InvalidAmount : CurrencyConversion
    data object Unavailable : CurrencyConversion
    data class Exact(val minor: BigInteger) : CurrencyConversion
    data class Approximate(val minor: BigInteger, val quote: ExchangeQuote) : CurrencyConversion
}
