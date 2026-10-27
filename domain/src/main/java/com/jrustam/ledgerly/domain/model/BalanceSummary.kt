package com.jrustam.ledgerly.domain.model

import java.math.BigInteger

data class BalanceSummary(
    val incomeMinor: BigInteger = BigInteger.ZERO,
    val expenseMinor: BigInteger = BigInteger.ZERO,
) {
    val balanceMinor: BigInteger get() = incomeMinor - expenseMinor
}
