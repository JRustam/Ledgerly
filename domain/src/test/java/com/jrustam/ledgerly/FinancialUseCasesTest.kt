package com.jrustam.ledgerly

import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.CurrencyConversion
import com.jrustam.ledgerly.domain.model.ExchangeQuote
import com.jrustam.ledgerly.domain.model.FinanceData
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.PlanKind
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.domain.repository.TransactionRepository
import com.jrustam.ledgerly.domain.usecase.CalculateLedgerBalancesUseCase
import com.jrustam.ledgerly.domain.usecase.ConvertCurrencyUseCase
import com.jrustam.ledgerly.domain.usecase.ExportTransactionsUseCase
import com.jrustam.ledgerly.domain.usecase.GetFinancialSummaryUseCase
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FinancialUseCasesTest {
    private val date = LocalDate.of(2026, 10, 24)
    private fun transaction(id: String, amount: Long, currency: String = "USD", wallet: String? = null) =
        Transaction(id, amount, currency, TransactionType.EXPENSE, Category.FOOD, date, null, wallet)

    @Test fun ledgerIncludesOpeningBalancesAndLegacyRowsExactlyOnce() {
        val wallets = listOf(
            Wallet("cash", "Cash", "", "CASH", "USD", 10000, 0, ""),
            Wallet("bank", "Bank", "", "BANK", "USD", 20000, 1, ""),
        )
        val data = FinanceData(wallets, listOf(Transfer("move", "cash", "bank", 1000, 1000, date)))
        val rows = listOf(transaction("spent", 2500, wallet = "cash"), transaction("legacy", 500), transaction("eur", 700, "EUR"))
        val result = CalculateLedgerBalancesUseCase()(data, rows)
        assertEquals(BigInteger("6500"), result.wallets["cash"])
        assertEquals(BigInteger("21000"), result.wallets["bank"])
        assertEquals(BigInteger("27000"), result.currencies["USD"])
        assertEquals(BigInteger("-700"), result.currencies["EUR"])
    }

    @Test fun reportSeparatesCurrenciesAndComputesCalendarMonthComparisons() {
        val rows = listOf(transaction("current", 8000), transaction("prior", 2000).copy(date = date.minusMonths(1)), transaction("other", 99999, "EUR"))
        val plans = listOf(
            FinancePlan("budget", PlanKind.BUDGET, "Food", "USD", 10000, Category.FOOD, date),
            FinancePlan("monthly", PlanKind.RECURRING, "Bill", "USD", 1200, Category.BILLS, date.minusMonths(1)),
            FinancePlan("future", PlanKind.RECURRING, "Future", "USD", 9900, Category.BILLS, date.plusMonths(1)),
        )
        val summary = GetFinancialSummaryUseCase()(rows, plans, YearMonth.from(date), "USD")
        assertEquals(BigInteger("8000"), summary.totals.expenseMinor)
        assertEquals(BigInteger("2000"), summary.previousTotals.expenseMinor)
        assertEquals(mapOf(Category.FOOD to BigInteger("8000")), summary.expensesByCategory)
        assertEquals(listOf("Food"), summary.budgetWarnings)
        assertEquals(BigInteger("1200"), summary.recurringMinor)
        assertEquals(6, summary.trend.size)
        assertEquals(BigInteger("8000"), summary.trend.last().amountMinor)
    }

    @Test fun emptyReportHasSixZeroMonthsAndNoWarnings() {
        val summary = GetFinancialSummaryUseCase()(emptyList(), emptyList(), YearMonth.from(date), "UAH")
        assertFalse(summary.hasTransactions)
        assertTrue(summary.trend.all { it.amountMinor == BigInteger.ZERO })
        assertTrue(summary.expensesByCategory.isEmpty())
        assertTrue(summary.budgetWarnings.isEmpty())
    }

    @Test fun converterRejectsMismatchedRatesAndInvalidAmounts() {
        val convert = ConvertCurrencyUseCase()
        val quote = ExchangeQuote("USD", "EUR", BigDecimal("0.895"), 100, "2026-10-24")
        assertEquals(CurrencyConversion.Unavailable, convert("1", "USD", "RSD", quote))
        assertEquals(CurrencyConversion.InvalidAmount, convert("1.001", "USD", "EUR", quote))
        assertEquals(CurrencyConversion.Exact(BigInteger("100")), convert("1", "USD", "USD", null))
        assertEquals(BigInteger("268"), (convert("3", "USD", "EUR", quote) as CurrencyConversion.Approximate).minor)
    }

    @Test fun exportUsesRepositorySnapshotAndEscapesUserText() = runTest {
        val snapshot = listOf(transaction("one", 123).copy(description = "=SUM(1,2)"))
        val repository = object : TransactionRepository {
            override fun observeAll() = flowOf(snapshot)
            override suspend fun findById(id: String): Transaction? = error("Not used")
            override suspend fun update(transaction: Transaction): Unit = error("Not used")
            override suspend fun delete(id: String): Unit = error("Not used")
            override suspend fun insert(transaction: Transaction): Unit = error("Not used")
        }
        val csv = ExportTransactionsUseCase(repository)()
        assertTrue(csv.contains("\"1.23\",\"USD\""))
        assertTrue(csv.contains("\"'=SUM(1,2)\""))
    }
}
