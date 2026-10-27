package com.jrustam.ledgerly.domain.repository

interface BankConnectionProvider {
    val liveConnectionAvailable: Boolean
}
