package com.jrustam.ledgerly.domain.model

/** Transfers retain both independently entered amounts; no implicit exchange rate. */
data class Wallet(
    val id: String, val name: String, val bank: String, val kind: String,
    val currency: String, val openingMinor: Long, val theme: Int, val lastFour: String
)
