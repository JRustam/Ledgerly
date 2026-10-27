package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Capture a consistent transaction snapshot and format CSV away from the UI dispatcher. */
class ExportTransactionsUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(): String = withContext(Dispatchers.Default) {
        transactionCsv(repository.observeAll().first())
    }
}
