package com.jrustam.ledgerly

import androidx.lifecycle.SavedStateHandle
import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.addtransaction.AddTransactionViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun preventsDoubleSaveAndResetsAfterSuccess() = runTest(dispatcher) {
        val transactions = mutableListOf<Transaction>()
        val vm = AddTransactionViewModel(AddTransaction(object : FakeTransactionRepository() {
            override suspend fun insert(transaction: Transaction) { delay(100); transactions += transaction }
        }), SavedStateHandle(), EditTransaction(FakeTransactionRepository()), FakeCurrencyPreferences())
        advanceUntilIdle()
        vm.edit(amount = "25", category = Category.FOOD)
        vm.save(); vm.save()
        assertTrue(vm.state.value.isSaving)
        advanceUntilIdle()
        assertEquals(1, transactions.size)
        assertTrue(vm.state.value.saved)
        assertEquals("", vm.state.value.amount)
        assertFalse(vm.state.value.isSaving)
    }

    @Test fun failedSaveKeepsDraftAndAllowsRetry() = runTest(dispatcher) {
        var fail = true
        val vm = AddTransactionViewModel(AddTransaction(object : FakeTransactionRepository() {
            override suspend fun insert(transaction: Transaction) { if (fail) error("Disk full") }
        }), SavedStateHandle(), EditTransaction(FakeTransactionRepository()), FakeCurrencyPreferences())
        advanceUntilIdle()
        vm.edit(amount = "25", category = Category.FOOD)
        vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value.saveFailed)
        assertEquals("25", vm.state.value.amount)
        fail = false
        vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value.saved)
    }

    @Test fun draftRestoresAndTypeChangeClearsCategory() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val add = AddTransaction(object : FakeTransactionRepository() {
            override suspend fun insert(transaction: Transaction) = Unit
        })
        val first = AddTransactionViewModel(add, handle, EditTransaction(FakeTransactionRepository()), FakeCurrencyPreferences())
        advanceUntilIdle()
        first.edit(amount = "18.50", category = Category.FOOD, description = "Dinner")
        val restored = AddTransactionViewModel(add, handle, EditTransaction(FakeTransactionRepository()), FakeCurrencyPreferences())
        assertEquals("18.50", restored.state.value.amount)
        assertEquals(Category.FOOD, restored.state.value.category)
        restored.edit(type = TransactionType.INCOME)
        assertNull(restored.state.value.category)
    }
}
