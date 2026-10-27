package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.calculation.budgetSpent
import com.jrustam.ledgerly.domain.calculation.calculateBalance
import com.jrustam.ledgerly.domain.calculation.monthlyTransactions
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.FinancialSummary
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.model.MonthExpense
import com.jrustam.ledgerly.domain.model.PlanKind
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import java.math.BigInteger
import java.time.YearMonth
import javax.inject.Inject

/** All comparisons use recorded calendar-month totals in one currency; no implicit conversion. */
class GetFinancialSummaryUseCase @Inject constructor() {
    operator fun invoke(rows: List<Transaction>, plans: List<FinancePlan>, month: YearMonth, currency: String): FinancialSummary {
        require(currency in LedgerCurrency.entries.map { it.name })
        val relevant = rows.filter { it.currency == currency }
        val current = monthlyTransactions(relevant, month, currency)
        val categories = current.filter { it.type == TransactionType.EXPENSE }.groupBy(Transaction::category)
            .mapValues { (_, entries) -> entries.fold(BigInteger.ZERO) { sum, row -> sum + BigInteger.valueOf(row.amountMinor) } }
            .entries.sortedByDescending { it.value }.associate { it.toPair() }
        val budgets = plans.filter { it.kind == PlanKind.BUDGET && it.currency == currency && YearMonth.from(it.date) == month }
        return FinancialSummary(
            month, currency, calculateBalance(current, currency),
            calculateBalance(monthlyTransactions(relevant, month.minusMonths(1), currency), currency),
            categories,
            (5L downTo 0L).map { offset ->
                val period = month.minusMonths(offset)
                MonthExpense(period, calculateBalance(monthlyTransactions(relevant, period, currency), currency).expenseMinor)
            },
            budgets.filter { budgetSpent(it, relevant) * BigInteger.TEN >= BigInteger.valueOf(it.amountMinor) * BigInteger.valueOf(8) }.map(FinancePlan::name),
            plans.filter { it.kind == PlanKind.RECURRING && it.currency == currency && !YearMonth.from(it.date).isAfter(month) }
                .fold(BigInteger.ZERO) { sum, plan -> sum + BigInteger.valueOf(plan.amountMinor) },
            current.isNotEmpty(),
        )
    }
}
