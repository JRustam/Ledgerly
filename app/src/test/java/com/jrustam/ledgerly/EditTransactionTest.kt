package com.jrustam.ledgerly

import androidx.lifecycle.SavedStateHandle
import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.addtransaction.AddTransactionViewModel
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class EditTransactionTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeTransactionRepository()
    private val original = Transaction(
        "original", 1234, "USD", TransactionType.EXPENSE, Category.FOOD,
        LocalDate.of(2025, 5, 5), "Lunch"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher); repository.rows.value = listOf(original)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun editor(handle: SavedStateHandle = SavedStateHandle(mapOf("transactionId" to original.id))) =
        AddTransactionViewModel(
            AddTransaction(repository),
            handle,
            EditTransaction(repository),
            FakeCurrencyPreferences()
        )

    @Test
    fun loadsAndEditsExistingRecordWithoutInserting() = runTest(dispatcher) {
        val vm = editor()
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertEquals("12.34", vm.state.value.amount)
        assertEquals(original.date, vm.state.value.date)
        vm.edit(amount = "15.25", type = TransactionType.INCOME)
        vm.edit(category = Category.GIFT)
        vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        assertEquals(1, repository.rows.value.size)
        assertEquals(original.id, repository.rows.value.single().id)
        assertEquals(1525L, repository.rows.value.single().amountMinor)
        assertEquals(TransactionType.INCOME, repository.rows.value.single().type)
    }

    @Test
    fun preservesUnsavedEditsAcrossRecreation() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("transactionId" to original.id))
        val first = editor(handle); advanceUntilIdle()
        first.edit(amount = "99.99", description = "Changed draft")
        val restored = editor(handle); advanceUntilIdle()
        assertEquals("99.99", restored.state.value.amount)
        assertEquals("Changed draft", restored.state.value.description)
        assertTrue(restored.state.value.isEditing)
    }

    @Test
    fun deletedRecordIsNotRecreatedBySavingStaleEditor() = runTest(dispatcher) {
        val vm = editor(); advanceUntilIdle()
        repository.delete(original.id)
        vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value.notFound)
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun editingUsesSameValidationAsAdding() = runTest(dispatcher) {
        val vm = editor(); advanceUntilIdle()
        vm.edit(amount = "0")
        vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value.errors.amount)
        assertEquals(original, repository.rows.value.single())
    }
}
