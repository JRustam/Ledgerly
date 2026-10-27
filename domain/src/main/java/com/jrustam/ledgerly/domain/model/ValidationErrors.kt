package com.jrustam.ledgerly.domain.model

data class ValidationErrors(
    val amount: Boolean = false, val category: Boolean = false,
    val description: Boolean = false,
) { val hasErrors: Boolean get() = amount || category || description }
