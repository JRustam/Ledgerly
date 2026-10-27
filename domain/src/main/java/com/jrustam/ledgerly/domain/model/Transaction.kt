package com.jrustam.ledgerly.domain.model

import java.time.LocalDate

data class Transaction(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val type: TransactionType,
    val category: Category,
    val date: LocalDate,
    val description: String?,
    val walletId: String? = null,
)
