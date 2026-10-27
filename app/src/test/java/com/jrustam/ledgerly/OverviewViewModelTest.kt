package com.jrustam.ledgerly

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.ui.overview.*
import java.math.BigInteger
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    private val viewModels = mutableListOf<OverviewViewModel>()

    @After
    fun teardown() {
        try {
            runTest(dispatcher) {
                // Await Default-dispatcher flow cancellation before replacing Main.
                viewModels.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private val income = Transaction(
        "income",
        10000,
        "USD",
        TransactionType.INCOME,
        Category.SALARY,
        LocalDate.now(),
        null
    )
    private val expense = Transaction(
        "expense",
        2500,
        "USD",
        TransactionType.EXPENSE,
        Category.FOOD,
        LocalDate.now(),
        null
    )

    @Test
    fun observesChangesAndFiltersWithoutChangingHomeTotals() = runTest(dispatcher) {
        val repo = FakeTransactionRepository()
        val vm = OverviewViewModel(repo, SavedStateHandle()).also(viewModels::add)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.state.first { !it.loading }
        repo.rows.value = listOf(income, expense)
        val loaded = vm.state.first { it.transactions.size == 2 }
        assertEquals(BigInteger("7500"), loaded.summaries["USD"]?.balanceMinor)
        vm.setFilter(TransactionFilter.EXPENSE)
        val filtered = vm.state.first { it.filter == TransactionFilter.EXPENSE }
        assertEquals(listOf(expense), filtered.visibleTransactions)
        assertEquals(BigInteger("7500"), filtered.summaries["USD"]?.balanceMinor)
        repo.update(expense.copy(amountMinor = 3500))
        vm.state.first { it.summaries["USD"]?.balanceMinor == BigInteger("6500") }
        vm.requestDelete(income)
        assertEquals(2, repo.rows.value.size)
        vm.cancelDelete()
        assertEquals(2, repo.rows.value.size)
        vm.requestDelete(income); vm.confirmDelete()
        val deleted = vm.state.first { it.transactions.size == 1 && it.pendingDelete == null }
        assertEquals(BigInteger("-3500"), deleted.summaries["USD"]?.balanceMinor)
    }

    @Test
    fun readFailureCanBeRetried() = runTest(dispatcher) {
        var fail = true
        val repo = object : FakeTransactionRepository() {
            override fun observeAll(): Flow<List<Transaction>> = flow {
                if (fail) error("Read failed") else emit(listOf(income))
            }
        }
        val vm = OverviewViewModel(repo, SavedStateHandle()).also(viewModels::add)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        assertTrue(vm.state.first { it.failed }.failed)
        fail = false; vm.retry()
        assertEquals(listOf(income), vm.state.first { it.transactions.isNotEmpty() }.transactions)
    }

    @Test
    fun deleteFailureRetainsRecordAndConfirmationForRetry() = runTest(dispatcher) {
        var fail = true
        val repo = object : FakeTransactionRepository() {
            override suspend fun delete(id: String) {
                if (fail) error("Write failed") else super.delete(id)
            }
        }
        repo.rows.value = listOf(income)
        val vm = OverviewViewModel(repo, SavedStateHandle()).also(viewModels::add)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.state.first { !it.loading }
        vm.requestDelete(income); vm.confirmDelete()
        assertEquals(income, vm.state.first { it.deleteFailed }.pendingDelete)
        assertEquals(listOf(income), repo.rows.value)
        fail = false; vm.confirmDelete()
        vm.state.first { it.pendingDelete == null && it.transactions.isEmpty() }
    }
}
