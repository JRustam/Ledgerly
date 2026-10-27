package com.jrustam.ledgerly.domain.calculation

import com.jrustam.ledgerly.domain.model.BalanceSummary
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import java.math.BigInteger
import java.time.YearMonth

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

fun monthlyTransactions(rows: List<Transaction>, month: YearMonth, currency: String) =
    rows.filter { YearMonth.from(it.date) == month && it.currency == currency }
