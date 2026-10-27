package com.jrustam.ledgerly.domain.repository

class ManualBankConnectionProvider : BankConnectionProvider {
    override val liveConnectionAvailable = false
}
