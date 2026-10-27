package com.jrustam.ledgerly.domain.repository

import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.FinanceData
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun observe(): Flow<FinanceData>
    suspend fun saveWallet(wallet: Wallet)
    suspend fun transfer(transfer: Transfer)
    suspend fun deleteTransfer(id: String)
    suspend fun savePlan(plan: FinancePlan)
    suspend fun deletePlan(id: String)
    suspend fun contribute(contribution: Contribution)
}
