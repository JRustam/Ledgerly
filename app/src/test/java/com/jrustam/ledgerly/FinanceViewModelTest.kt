package com.jrustam.ledgerly

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.FinanceData
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.domain.repository.FinanceRepository
import com.jrustam.ledgerly.domain.repository.TransactionRepository
import com.jrustam.ledgerly.domain.usecase.CalculateLedgerBalancesUseCase
import com.jrustam.ledgerly.domain.usecase.GetFinancialSummaryUseCase
import com.jrustam.ledgerly.ui.finance.FinanceViewModel
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<FinanceViewModel>()
    private class Repository : FinanceRepository {
        val rows = MutableStateFlow(FinanceData())
        var failWrites = false
        override fun observe() = rows
        override suspend fun saveWallet(wallet: Wallet) {
            check(!failWrites)
            rows.value = rows.value.copy(wallets = listOf(wallet))
        }
        override suspend fun transfer(transfer: Transfer) = error("Not used")
        override suspend fun deleteTransfer(id: String) = error("Not used")
        override suspend fun savePlan(plan: FinancePlan) = error("Not used")
        override suspend fun deletePlan(id: String) = error("Not used")
        override suspend fun contribute(contribution: Contribution) = error("Not used")
    }
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        try { runTest(dispatcher) { models.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } } }
        finally { Dispatchers.resetMain() }
    }
    private fun model(finance: Repository, transactions: TransactionRepository, state: SavedStateHandle) =
        FinanceViewModel(finance, transactions, CalculateLedgerBalancesUseCase(), GetFinancialSummaryUseCase(), state).also(models::add)

    @Test fun restoredReportMonthAndWalletBalancesFollowRepositoryUpdates() = runTest(dispatcher) {
        val finance = Repository()
        val transactions = FakeTransactionRepository()
        val saved = SavedStateHandle(mapOf("reportMonthOffset" to -1))
        val vm = model(finance, transactions, saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        val initial = vm.state.first { !it.loading }
        assertEquals(YearMonth.now().minusMonths(1), initial.reports.getValue("USD").month)
        finance.rows.value = FinanceData(wallets = listOf(Wallet("cash", "Cash", "", "CASH", "USD", 10000, 0, "")))
        transactions.rows.value = listOf(Transaction("expense", 2500, "USD", TransactionType.EXPENSE, Category.FOOD, LocalDate.now(), null, "cash"))
        val updated = vm.state.first { it.balances.currencies["USD"] == BigInteger("7500") }
        assertEquals(BigInteger("2500"), updated.currentMonthTotals.getValue("USD").expenseMinor)
        vm.selectMonth(0)
        val current = vm.state.first { it.monthOffset == 0 }
        assertEquals(BigInteger("2500"), current.reports.getValue("USD").totals.expenseMinor)
        assertEquals(0, saved.get<Int>("reportMonthOffset"))
    }

    @Test fun failedWriteDoesNotInvokeSuccessAndCanBeRetried() = runTest(dispatcher) {
        val finance = Repository().apply { failWrites = true }
        val vm = model(finance, FakeTransactionRepository(), SavedStateHandle())
        val wallet = Wallet("cash", "Cash", "", "CASH", "USD", 0, 0, "")
        var completions = 0
        vm.save(wallet) { completions++ }
        runCurrent()
        assertTrue(vm.error.value)
        assertFalse(vm.busy.value)
        assertEquals(0, completions)
        finance.failWrites = false
        vm.save(wallet) { completions++ }
        runCurrent()
        assertFalse(vm.error.value)
        assertEquals(1, completions)
        assertEquals(listOf(wallet), finance.rows.value.wallets)
    }
}
