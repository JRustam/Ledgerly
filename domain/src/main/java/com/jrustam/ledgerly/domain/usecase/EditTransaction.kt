package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionDraft
import com.jrustam.ledgerly.domain.repository.TransactionRepository
import javax.inject.Inject

class EditTransaction @Inject constructor(private val repository: TransactionRepository) {
    suspend fun load(id: String): Transaction = repository.findById(id) ?: throw TransactionNotFound()

    suspend fun save(draft: TransactionDraft) {
        load(draft.id)
        repository.update(AddTransaction.validate(draft))
    }
}
