package com.jrustam.ledgerly

import com.jrustam.ledgerly.domain.*
import kotlinx.coroutines.flow.MutableStateFlow

open class FakeTransactionRepository : TransactionRepository {
    val rows = MutableStateFlow<List<Transaction>>(emptyList())
    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<Transaction>> = rows
    override suspend fun findById(id: String) = rows.value.find { it.id == id }
    override suspend fun insert(transaction: Transaction) {
        if (findById(transaction.id) == null) rows.value += transaction
    }

    override suspend fun update(transaction: Transaction) {
        if (findById(transaction.id) == null) throw TransactionNotFound()
        rows.value = rows.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
