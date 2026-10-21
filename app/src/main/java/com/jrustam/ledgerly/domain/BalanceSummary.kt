package com.jrustam.ledgerly.domain

import java.math.BigInteger

data class BalanceSummary(
    val incomeMinor: BigInteger = BigInteger.ZERO,
    val expenseMinor: BigInteger = BigInteger.ZERO,
) {
    val balanceMinor: BigInteger get() = incomeMinor - expenseMinor
}

/** All-time totals. BigInteger prevents overflow when many valid Long amounts are summed. */
fun calculateBalance(transactions: List<Transaction>, currency: String = "USD"): BalanceSummary {
    var income = BigInteger.ZERO
    var expenses = BigInteger.ZERO
    transactions.forEach {
        require(it.currency == currency) { "Do not combine different currencies" }
        require(it.amountMinor > 0) { "Transaction amounts must be positive" }
        when (it.type) {
            TransactionType.INCOME -> income += BigInteger.valueOf(it.amountMinor)
            TransactionType.EXPENSE -> expenses += BigInteger.valueOf(it.amountMinor)
        }
    }
    return BalanceSummary(income, expenses)
}

/** No conversion: each ISO currency has its own independent ledger totals. */
fun calculateBalances(transactions: List<Transaction>): Map<String, BalanceSummary> =
    transactions.groupBy { it.currency }.toSortedMap().mapValues { (currency, rows) ->
        calculateBalance(rows, currency)
    }
