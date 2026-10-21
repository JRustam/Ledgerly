package com.jrustam.ledgerly.data

import com.jrustam.ledgerly.domain.*
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> = dao.observeAll().map { rows ->
        rows.map { it.toDomain() }
    }

    override suspend fun findById(id: String): Transaction? = dao.findById(id)?.toDomain()
    override suspend fun insert(transaction: Transaction) {
        dao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction) {
        // Never recreate a record that was deleted while its editor was open.
        if (dao.update(transaction.toEntity()) == 0) throw TransactionNotFound()
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}

private fun Transaction.toEntity() = TransactionEntity(
    id, amountMinor, currency, type.name, category.name, date.toEpochDay(), description,
)

private fun TransactionEntity.toDomain() = Transaction(
    id, amountMinor, currency, TransactionType.valueOf(type), Category.valueOf(category),
    LocalDate.ofEpochDay(dateEpochDay), description,
)
