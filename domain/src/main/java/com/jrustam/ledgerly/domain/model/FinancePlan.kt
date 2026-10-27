package com.jrustam.ledgerly.domain.model

import java.time.LocalDate

data class FinancePlan(
    val id: String, val kind: PlanKind, val name: String, val currency: String,
    val amountMinor: Long, val category: Category, val date: LocalDate
)
