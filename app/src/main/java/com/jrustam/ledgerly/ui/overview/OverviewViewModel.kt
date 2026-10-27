package com.jrustam.ledgerly.ui.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.calculation.calculateBalances
import com.jrustam.ledgerly.domain.model.BalanceSummary
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TransactionFilter { ALL, INCOME, EXPENSE }

data class OverviewState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val summaries: Map<String, BalanceSummary> = emptyMap(),
    val filter: TransactionFilter = TransactionFilter.ALL,
    val pendingDelete: Transaction? = null,
    val deleting: Boolean = false,
    val deleteFailed: Boolean = false,
) {
    val visibleTransactions: List<Transaction> get() = transactions.filter {
        filter == TransactionFilter.ALL || it.type.name == filter.name
    }
}

private data class DeleteState(
    val transaction: Transaction? = null, val busy: Boolean = false, val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val filter = savedState.getStateFlow("filter", TransactionFilter.ALL.name)
    private val deletion = MutableStateFlow(DeleteState())
    private val ledger = refresh.flatMapLatest {
        repository.observeAll().map { rows ->
            OverviewState(loading = false, transactions = rows, summaries = calculateBalances(rows))
        }.onStart { emit(OverviewState()) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(OverviewState(loading = false, failed = true))
            }
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<OverviewState> = combine(ledger, filter, deletion) { data, selected, action ->
        data.copy(filter = TransactionFilter.valueOf(selected), pendingDelete = action.transaction,
            deleting = action.busy, deleteFailed = action.failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewState())

    fun setFilter(value: TransactionFilter) { savedState["filter"] = value.name }
    fun retry() { refresh.value += 1 }
    fun requestDelete(transaction: Transaction) {
        if (!deletion.value.busy) deletion.value = DeleteState(transaction)
    }
    fun cancelDelete() { if (!deletion.value.busy) deletion.value = DeleteState() }
    fun confirmDelete() {
        val pending = deletion.value
        if (pending.busy || pending.transaction == null) return
        deletion.value = pending.copy(busy = true, failed = false)
        viewModelScope.launch {
            try {
                repository.delete(pending.transaction.id)
                deletion.value = DeleteState()
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { deletion.value = pending.copy(failed = true) }
        }
    }
}
