package com.jrustam.ledgerly.domain.model

import java.math.BigDecimal

data class ExchangeQuote(
    val base: String,
    val target: String,
    val rate: BigDecimal,
    val fetchedAtMillis: Long,
    val rateDate: String = ""
)
