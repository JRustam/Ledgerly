package com.jrustam.ledgerly.domain.repository

import com.jrustam.ledgerly.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<Transaction>>
    suspend fun findById(id: String): Transaction?
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
    suspend fun insert(transaction: Transaction)
}
