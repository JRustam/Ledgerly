package com.jrustam.ledgerly.ui.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.calculation.calculateBalances
import com.jrustam.ledgerly.domain.model.BalanceSummary
import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.FinanceData
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.FinancialSummary
import com.jrustam.ledgerly.domain.model.LedgerBalances
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.domain.repository.FinanceRepository
import com.jrustam.ledgerly.domain.repository.TransactionRepository
import com.jrustam.ledgerly.domain.usecase.CalculateLedgerBalancesUseCase
import com.jrustam.ledgerly.domain.usecase.GetFinancialSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import java.time.YearMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FinanceState(val loading: Boolean = true, val failed: Boolean = false,
    val data: FinanceData = FinanceData(), val transactions: List<Transaction> = emptyList(),
    val balances: LedgerBalances = LedgerBalances(emptyMap(), emptyMap(), emptyMap()),
    val monthOffset: Int = 0,
    val reports: Map<String, FinancialSummary> = emptyMap(),
    val currentMonthTotals: Map<String, BalanceSummary> = emptyMap(),
)
@HiltViewModel
class FinanceViewModel @Inject constructor(private val repository: FinanceRepository,
    private val transactions: TransactionRepository,
    private val balances: CalculateLedgerBalancesUseCase,
    private val financialSummary: GetFinancialSummaryUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val monthOffset = savedState.getStateFlow("reportMonthOffset", 0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = refresh.flatMapLatest {
        combine(repository.observe(), transactions.observeAll(), monthOffset) { data, rows, offset ->
            val currentMonth = YearMonth.now()
            val selectedMonth = currentMonth.plusMonths(offset.toLong())
            FinanceState(
                loading = false, data = data, transactions = rows,
                balances = balances(data, rows), monthOffset = offset,
                reports = LedgerCurrency.entries.associate { currency ->
                    currency.name to financialSummary(rows, data.plans, selectedMonth, currency.name)
                },
                currentMonthTotals = calculateBalances(rows.filter { YearMonth.from(it.date) == currentMonth }),
            )
        }
            .onStart { emit(FinanceState()) }.catch { if (it is CancellationException) throw it; emit(FinanceState(false, true)) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceState())
    fun selectMonth(offset: Int) { savedState["reportMonthOffset"] = offset.coerceIn(-1200, 0) }
    fun retry() { refresh.value++ }
    fun dismissError() { mutableError.value = false }
    private fun write(done: () -> Unit, action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true; mutableError.value = false
        viewModelScope.launch {
            try { action(); done() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableError.value = true }
            finally { mutableBusy.value = false }
        }
    }
    fun save(wallet: Wallet, done: () -> Unit) = write(done) { repository.saveWallet(wallet) }
    fun save(plan: FinancePlan, done: () -> Unit) = write(done) { repository.savePlan(plan) }
    fun transfer(value: Transfer, done: () -> Unit) = write(done) { repository.transfer(value) }
    fun contribute(value: Contribution, done: () -> Unit) = write(done) { repository.contribute(value) }
    fun deletePlan(id: String, done: () -> Unit) = write(done) { repository.deletePlan(id) }
    fun deleteTransfer(id: String, done: () -> Unit) = write(done) { repository.deleteTransfer(id) }
}
