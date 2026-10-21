package com.jrustam.ledgerly.domain

import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class TransactionDraft(
    val id: String, val amount: String, val type: TransactionType,
    val category: Category?, val date: LocalDate, val description: String,
    val currency: String = "USD",
)

data class ValidationErrors(
    val amount: Boolean = false, val category: Boolean = false,
    val description: Boolean = false,
) { val hasErrors: Boolean get() = amount || category || description }

class InvalidTransaction(val errors: ValidationErrors) : IllegalArgumentException()

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
                description = draft.description.trim().ifEmpty { null },
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
