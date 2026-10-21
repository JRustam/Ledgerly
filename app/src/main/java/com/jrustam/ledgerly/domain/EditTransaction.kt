package com.jrustam.ledgerly.domain

import javax.inject.Inject

class TransactionNotFound : IllegalStateException()

class EditTransaction @Inject constructor(private val repository: TransactionRepository) {
    suspend fun load(id: String): Transaction = repository.findById(id) ?: throw TransactionNotFound()

    suspend fun save(draft: TransactionDraft) {
        load(draft.id)
        repository.update(AddTransaction.validate(draft))
    }
}
