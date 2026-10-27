package com.jrustam.ledgerly

import com.jrustam.ledgerly.domain.calculation.calculateBalance
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import java.math.BigInteger
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class BalanceSummaryTest {
    private fun transaction(amount: Long, type: TransactionType = TransactionType.INCOME) =
        Transaction(
            "id",
            amount,
            "USD",
            type,
            if (type == TransactionType.INCOME) Category.SALARY else Category.FOOD,
            LocalDate.of(2026, 1, 1),
            null,
        )

    @Test
    fun emptyLedgerHasZeroTotals() {
        val result = calculateBalance(emptyList())
        assertEquals(BigInteger.ZERO, result.balanceMinor)
        assertEquals(BigInteger.ZERO, result.incomeMinor)
        assertEquals(BigInteger.ZERO, result.expenseMinor)
    }

    @Test
    fun computesIncomeExpenseAndBalanceInExactCents() {
        val result = calculateBalance(
            listOf(
                transaction(10001), transaction(20),
                transaction(3002, TransactionType.EXPENSE), transaction(19, TransactionType.EXPENSE)
            )
        )
        assertEquals(BigInteger("10021"), result.incomeMinor)
        assertEquals(BigInteger("3021"), result.expenseMinor)
        assertEquals(BigInteger("7000"), result.balanceMinor)
    }

    @Test
    fun expenseOnlyLedgerHasNegativeBalance() {
        val result = calculateBalance(listOf(transaction(1, TransactionType.EXPENSE)))
        assertEquals(BigInteger("-1"), result.balanceMinor)
    }

    @Test
    fun totalsDoNotOverflowLong() {
        val maximumValidAmount = 999999999999999999L
        val result = calculateBalance(List(20) { transaction(maximumValidAmount) })
        assertEquals(BigInteger("19999999999999999980"), result.balanceMinor)
    }

    @Test
    fun editingTypeAndDeletingRecalculateTotals() {
        val income = transaction(500)
        val edited = income.copy(type = TransactionType.EXPENSE, category = Category.FOOD)
        assertEquals(BigInteger("500"), calculateBalance(listOf(income)).balanceMinor)
        assertEquals(BigInteger("-500"), calculateBalance(listOf(edited)).balanceMinor)
        assertEquals(BigInteger.ZERO, calculateBalance(emptyList()).balanceMinor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesToCombineCurrencies() {
        calculateBalance(listOf(transaction(10).copy(currency = "EUR")))
    }
}
