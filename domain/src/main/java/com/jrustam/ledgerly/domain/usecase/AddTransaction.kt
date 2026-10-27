package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionDraft
import com.jrustam.ledgerly.domain.model.ValidationErrors
import com.jrustam.ledgerly.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject

class AddTransaction @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(draft: TransactionDraft) {
        repository.insert(validate(draft))
    }

    companion object {
        fun validate(draft: TransactionDraft): Transaction {
            require(LedgerCurrency.entries.any { it.name == draft.currency })
            val minor = parseAmount(draft.amount)
            val errors = ValidationErrors(
                amount = minor == null,
                category = draft.category == null || draft.category.type != draft.type,
                description = draft.description.trim().length > 500,
            )
            if (errors.hasErrors) throw InvalidTransaction(errors)
            return Transaction(
                id = draft.id, amountMinor = requireNotNull(minor), currency = draft.currency,
                type = draft.type, category = requireNotNull(draft.category), date = draft.date,
                description = draft.description.trim().ifEmpty { null }, walletId = draft.walletId,
            )
        }

        // Accept decimal point or comma, without grouping, signs, or scientific notation.
        fun parseAmount(input: String): Long? {
            val value = input.trim().replace(',', '.')
            if (!Regex("[0-9]{1,16}(\\.[0-9]{1,2})?").matches(value)) return null
            return try {
                BigDecimal(value).movePointRight(2).longValueExact().takeIf { it > 0 }
            } catch (_: ArithmeticException) { null }
        }
    }
}
