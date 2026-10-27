package com.jrustam.ledgerly.domain.model

import java.math.BigInteger

data class LedgerBalances(
    val wallets: Map<String, BigInteger>,
    val currencies: Map<String, BigInteger>,
    val unassigned: Map<String, BalanceSummary>,
)
