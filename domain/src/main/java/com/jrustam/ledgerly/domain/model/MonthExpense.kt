package com.jrustam.ledgerly.domain.model

import java.math.BigInteger
import java.time.YearMonth

data class MonthExpense(val month: YearMonth, val amountMinor: BigInteger)
