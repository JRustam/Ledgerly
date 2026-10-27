package com.jrustam.ledgerly.domain.model

import java.time.LocalDate

data class TransactionDraft(
    val id: String, val amount: String, val type: TransactionType,
    val category: Category?, val date: LocalDate, val description: String,
    val currency: String = "USD",
    val walletId: String? = null,
)
