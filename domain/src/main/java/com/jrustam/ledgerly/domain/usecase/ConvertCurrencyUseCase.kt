package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.calculation.convertMinor
import com.jrustam.ledgerly.domain.model.CurrencyConversion
import com.jrustam.ledgerly.domain.model.ExchangeQuote
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import java.math.BigInteger
import javax.inject.Inject

/** Reject a quote for the wrong pair rather than label the result with another currency. */
class ConvertCurrencyUseCase @Inject constructor() {
    operator fun invoke(amount: String, base: String, target: String, quote: ExchangeQuote?): CurrencyConversion {
        val minor = AddTransaction.parseAmount(amount)?.let(BigInteger::valueOf) ?: return CurrencyConversion.InvalidAmount
        if (base !in LedgerCurrency.entries.map { it.name } || target !in LedgerCurrency.entries.map { it.name }) return CurrencyConversion.Unavailable
        if (base == target) return CurrencyConversion.Exact(minor)
        if (quote == null || quote.base != base || quote.target != target || quote.rate.signum() <= 0) return CurrencyConversion.Unavailable
        return CurrencyConversion.Approximate(convertMinor(minor, quote), quote)
    }
}
