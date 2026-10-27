package com.jrustam.ledgerly.data.repository

import com.jrustam.ledgerly.data.TransactionDatabase
import com.jrustam.ledgerly.data.local.dao.TransactionDao
import com.jrustam.ledgerly.data.local.entity.TransactionEntity
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.repository.TransactionRepository
import com.jrustam.ledgerly.domain.usecase.TransactionNotFound
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.Category
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val database: TransactionDatabase,
) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> = dao.observeAll().map { rows ->
        rows.map { it.toDomain() }
    }

    override suspend fun findById(id: String): Transaction? = dao.findById(id)?.toDomain()
    override suspend fun insert(transaction: Transaction) {
        validateWallet(transaction)
        dao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction) {
        // Never recreate a record that was deleted while its editor was open.
        validateWallet(transaction)
        if (dao.update(transaction.toEntity()) == 0) throw TransactionNotFound()
    }

    private suspend fun validateWallet(transaction: Transaction) {
        transaction.walletId?.let { id ->
            require(database.financeDao().wallet(id)?.currency == transaction.currency)
        }
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

private fun Transaction.toEntity() = TransactionEntity(
    id, amountMinor, currency, type.name, category.name, date.toEpochDay(), description, walletId,
)

private fun TransactionEntity.toDomain() = Transaction(
    id, amountMinor, currency, TransactionType.valueOf(type), Category.valueOf(category),
    LocalDate.ofEpochDay(dateEpochDay), description, walletId,
)
