package com.jrustam.ledgerly.domain.calculation

import com.jrustam.ledgerly.domain.model.ExchangeQuote
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun convertMinor(amount: BigInteger, quote: ExchangeQuote): BigInteger {
    require(quote.rate.signum() > 0)
    return BigDecimal(amount).multiply(quote.rate).setScale(0, RoundingMode.HALF_EVEN)
        .toBigIntegerExact()
}
