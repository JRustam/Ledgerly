package com.jrustam.ledgerly.domain.model

import java.time.LocalDate

data class Contribution(
    val id: String,
    val goalId: String,
    val amountMinor: Long,
    val date: LocalDate
)
