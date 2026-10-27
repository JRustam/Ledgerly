package com.jrustam.ledgerly

import com.jrustam.ledgerly.domain.calculation.budgetSpent
import com.jrustam.ledgerly.domain.calculation.calculateBalance
import com.jrustam.ledgerly.domain.calculation.convertMinor
import com.jrustam.ledgerly.domain.calculation.goalEstimate
import com.jrustam.ledgerly.domain.calculation.goalSaved
import com.jrustam.ledgerly.domain.calculation.progressFraction
import com.jrustam.ledgerly.domain.calculation.upcomingPayments
import com.jrustam.ledgerly.domain.calculation.walletBalance
import com.jrustam.ledgerly.domain.model.AppLanguage
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.ExchangeQuote
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.PlanKind
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.domain.usecase.transactionCsv
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class FinanceTest {
    private val today = LocalDate.of(2026,10,21)
    private fun row(id: String, amount: Long, currency: String = "USD", wallet: String? = "a", date: LocalDate = today) =
        Transaction(id,amount,currency,TransactionType.EXPENSE,Category.FOOD,date,null,wallet)
    private val wallet = Wallet("a","Cash","","CASH","USD",10000,0,"")
    private val budget = FinancePlan("budget",PlanKind.BUDGET,"Food","USD",10000,Category.FOOD,today)
    @Test fun monthlyScheduleClampsFebruaryWithoutDriftingAndSkipsPastPayments() {
        assertEquals(listOf(LocalDate.of(2026,2,28),LocalDate.of(2026,3,31)),
            upcomingPayments(LocalDate.of(2025,1,31),LocalDate.of(2026,2,15),2))
    }
    @Test fun transfersMoveBalancesWithoutChangingIncomeOrExpenses() {
        val target = wallet.copy(id = "b",openingMinor = 0)
        val transfers = listOf(Transfer("t","a","b",3500,3500,today))
        assertEquals(BigInteger("6500"),walletBalance(wallet,emptyList(),transfers))
        assertEquals(BigInteger("3500"),walletBalance(target,emptyList(),transfers))
        assertEquals(BigInteger("10000"),walletBalance(wallet,emptyList(),transfers)+walletBalance(target,emptyList(),transfers))
        assertEquals(BigInteger.ZERO,calculateBalance(emptyList()).expenseMinor)
    }
    @Test fun crossCurrencyTransferRetainsBothAmounts() {
        val transfer = Transfer("fx","a","b",1000,1700,today)
        assertEquals(BigInteger("9000"),walletBalance(wallet,emptyList(),listOf(transfer)))
        assertEquals(BigInteger("1700"),walletBalance(wallet.copy(id="b",currency="AZN",openingMinor=0),emptyList(),listOf(transfer)))
    }
    @Test fun walletExcludesUnassignedOtherCurrenciesAndOtherAccounts() {
        val rows = listOf(row("1",500),row("2",900,wallet=null),row("3",800,currency="EUR"),row("4",600,wallet="b"))
        assertEquals(BigInteger("9500"),walletBalance(wallet,rows,emptyList()))
    }
    @Test fun budgetIncludesOnlyMatchingMonthCurrencyCategoryAndExpenses() {
        val rows = listOf(row("1",8000),row("2",3000),row("3",999,currency="EUR"),row("4",999,date=today.minusMonths(1)),
            row("5",999).copy(category=Category.BILLS),row("6",999).copy(type=TransactionType.INCOME,category=Category.SALARY))
        assertEquals(BigInteger("11000"),budgetSpent(budget,rows))
        assertEquals(1f,progressFraction(budgetSpent(budget,rows),BigInteger("10000")))
    }
    @Test fun goalForecastUsesObservedContributionPaceAndNeedsHistory() {
        val goal = budget.copy(id="goal",kind=PlanKind.GOAL)
        assertNull(goalEstimate(goal,emptyList(),today))
        assertNull(goalEstimate(goal,listOf(Contribution("one","goal",2000,today)),today))
        val history=listOf(Contribution("one","goal",2000,today.minusDays(10)))
        assertEquals(today.plusDays(40),goalEstimate(goal,history,today))
        assertEquals(BigInteger("2000"),goalSaved(goal,history+Contribution("other","else",90000,today)))
    }
    @Test fun conversionUsesExactDecimalAndBankersRounding() {
        val quote=ExchangeQuote("USD","EUR",BigDecimal("0.895"),123)
        assertEquals(BigInteger("90"),convertMinor(BigInteger("100"),quote))
        assertEquals(BigInteger("268"),convertMinor(BigInteger("300"),quote))
        assertEquals(BigInteger("895000000000000000000000"),convertMinor(BigInteger("1000000000000000000000000"),quote))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidRateCannotConvert() {
        convertMinor(BigInteger.TEN,ExchangeQuote("USD","EUR",BigDecimal.ZERO,0))
    }
    @Test fun languageSelectionPreservesSerbianScripts() {
        assertEquals(AppLanguage.SERBIAN_LATIN,AppLanguage.fromTag("sr-Latn"))
        assertEquals(AppLanguage.SERBIAN_CYRILLIC,AppLanguage.fromTag("sr-Cyrl"))
        assertEquals(AppLanguage.UKRAINIAN,AppLanguage.fromTag("uk-UA"))
    }
    @Test fun csvQuotesNotesAndNeutralizesSpreadsheetFormulas() {
        val csv=transactionCsv(listOf(row("a",123).copy(description="=HYPERLINK(\"bad\")\nnext")))
        assertTrue(csv.contains("\"1.23\",\"USD\""))
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"bad\"\")\nnext\""))
    }
    @Test fun aggregatesCannotOverflowLong() {
        val rows = listOf(row("a",Long.MAX_VALUE),row("b",Long.MAX_VALUE))
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE)*BigInteger.TWO,budgetSpent(budget,rows))
    }
}
