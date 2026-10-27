package com.jrustam.ledgerly.domain.model

data class FinanceData(
    val wallets: List<Wallet> = emptyList(), val transfers: List<Transfer> = emptyList(),
    val plans: List<FinancePlan> = emptyList(), val contributions: List<Contribution> = emptyList()
)
