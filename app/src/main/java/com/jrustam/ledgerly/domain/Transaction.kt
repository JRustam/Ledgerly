package com.jrustam.ledgerly.domain

import java.time.LocalDate

enum class TransactionType { EXPENSE, INCOME }

enum class Category(val type: TransactionType) {
    FOOD(TransactionType.EXPENSE), TRANSPORT(TransactionType.EXPENSE),
    SHOPPING(TransactionType.EXPENSE), BILLS(TransactionType.EXPENSE),
    HEALTH(TransactionType.EXPENSE), OTHER_EXPENSE(TransactionType.EXPENSE),
    SALARY(TransactionType.INCOME), FREELANCE(TransactionType.INCOME),
    GIFT(TransactionType.INCOME), OTHER_INCOME(TransactionType.INCOME)
}

data class Transaction(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val type: TransactionType,
    val category: Category,
    val date: LocalDate,
    val description: String?,
)

interface TransactionRepository {
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<Transaction>>
    suspend fun findById(id: String): Transaction?
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
    suspend fun insert(transaction: Transaction)
}
