package com.jrustam.ledgerly

import com.jrustam.ledgerly.domain.*
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AddTransactionTest {
    private val stored = mutableListOf<Transaction>()
    private val add = AddTransaction(object : FakeTransactionRepository() {
        override suspend fun insert(transaction: Transaction) {
            stored += transaction
        }
    })

    private fun draft() = TransactionDraft(
        "id", "12.34", TransactionType.EXPENSE,
        Category.FOOD, LocalDate.of(2026, 1, 2), "  Lunch  "
    )

    @Test
    fun exactMinorUnitsAndDecimalComma() {
        assertEquals(1234L, AddTransaction.parseAmount("12.34"))
        assertEquals(1234L, AddTransaction.parseAmount("12,34"))
        assertEquals(1L, AddTransaction.parseAmount("0.01"))
        assertEquals(999999999999999999L, AddTransaction.parseAmount("9999999999999999.99"))
    }

    @Test
    fun rejectsMalformedAndNonPositiveAmounts() {
        listOf("", "0", "-1", "1.001", "1e3", "1,234.50", "NaN", "99999999999999999").forEach {
            assertNull(it, AddTransaction.parseAmount(it))
        }
    }

    @Test
    fun savesValidatedTransaction() = runTest {
        add(draft())
        assertEquals(
            Transaction(
                "id", 1234, "USD", TransactionType.EXPENSE,
                Category.FOOD, LocalDate.of(2026, 1, 2), "Lunch"
            ), stored.single()
        )
    }

    @Test
    fun rejectsMissingOrMismatchedCategoryAndLongDescription() = runTest {
        for (invalid in listOf(
            draft().copy(category = null), draft().copy(category = Category.SALARY),
            draft().copy(description = "a".repeat(501)), draft().copy(amount = "0")
        )) {
            try {
                add(invalid); fail("Expected validation failure")
            } catch (error: InvalidTransaction) {
                assertTrue(error.errors.hasErrors)
            }
        }
        assertTrue(stored.isEmpty())
    }

    @Test
    fun blankDescriptionIsNull() = runTest {
        add(draft().copy(description = "  "))
        assertNull(stored.single().description)
    }
}
