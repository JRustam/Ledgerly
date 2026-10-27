package com.jrustam.ledgerly.domain.model

import java.math.BigInteger
import java.time.YearMonth

data class FinancialSummary(
    val month: YearMonth,
    val currency: String,
    val totals: BalanceSummary,
    val previousTotals: BalanceSummary,
    val expensesByCategory: Map<Category, BigInteger>,
    val trend: List<MonthExpense>,
    val budgetWarnings: List<String>,
    val recurringMinor: BigInteger,
    val hasTransactions: Boolean,
)
