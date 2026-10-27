package com.jrustam.ledgerly.domain.model

import java.time.LocalDate

data class Transfer(
    val id: String, val fromId: String, val toId: String, val debitMinor: Long,
    val creditMinor: Long, val date: LocalDate
)
