package com.jrustam.ledgerly.ui.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.model.TransactionDraft
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.ValidationErrors
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.domain.repository.CurrencyPreferences
import com.jrustam.ledgerly.domain.usecase.AddTransaction
import com.jrustam.ledgerly.domain.usecase.EditTransaction
import com.jrustam.ledgerly.domain.usecase.InvalidTransaction
import com.jrustam.ledgerly.domain.usecase.TransactionNotFound
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AddTransactionState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val notFound: Boolean = false,
    val currency: LedgerCurrency = LedgerCurrency.USD,
    val walletId: String? = null,
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category? = null,
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val errors: ValidationErrors = ValidationErrors(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val saveFailed: Boolean = false,
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransaction: AddTransaction,
    private val savedState: SavedStateHandle,
    private val editTransaction: EditTransaction,
    private val preferences: CurrencyPreferences,
) : ViewModel() {
    private val editId: String? = savedState["transactionId"]
    private var draftId: String = savedState.get<String>("id") ?: UUID.randomUUID().toString()
    private val mutableState = MutableStateFlow(
        AddTransactionState(
            currency = savedState.get<String>("currency")?.let(LedgerCurrency::valueOf)
                ?: LedgerCurrency.USD,
            walletId = savedState["walletId"],
            amount = savedState["amount"] ?: "",
            type = savedState.get<String>("type")?.let(TransactionType::valueOf)
                ?: TransactionType.EXPENSE,
            category = savedState.get<String>("category")?.let(Category::valueOf),
            date = savedState.get<Long>("date")?.let(LocalDate::ofEpochDay) ?: LocalDate.now(),
            description = savedState["description"] ?: "",
        )
    )
    val state = mutableState.asStateFlow()

    init {
        if (editId == null) {
            if (savedState.contains("amount")) persist() else load()
        } else if (savedState.get<Boolean>("editLoaded") == true) {
            draftId = editId
            mutableState.value = mutableState.value.copy(isEditing = true)
        } else load()
    }

    fun load() {
        val id = editId
        if (mutableState.value.isLoading) return
        mutableState.value = mutableState.value.copy(
            isEditing = editId != null,
            isLoading = true,
            loadFailed = false,
            notFound = false
        )
        viewModelScope.launch {
            try {
                if (id == null) {
                    mutableState.value = mutableState.value.copy(
                        currency = preferences.defaultCurrency.first(),
                        isLoading = false
                    )
                    persist()
                    return@launch
                }
                val transaction = editTransaction.load(id)
                draftId = transaction.id
                mutableState.value = AddTransactionState(
                    isEditing = true,
                    currency = LedgerCurrency.valueOf(transaction.currency),
                    amount = java.math.BigDecimal.valueOf(transaction.amountMinor, 2)
                        .toPlainString(),
                    walletId = transaction.walletId,
                    type = transaction.type,
                    category = transaction.category,
                    date = transaction.date,
                    description = transaction.description.orEmpty(),
                )
                savedState["editLoaded"] = true
                persist()
            } catch (error: CancellationException) {
                throw error
            } catch (_: TransactionNotFound) {
                mutableState.value = mutableState.value.copy(isLoading = false, notFound = true)
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(isLoading = false, loadFailed = true)
            }
        }
    }

    fun edit(
        amount: String? = null, type: TransactionType? = null,
        category: Category? = null, date: LocalDate? = null, description: String? = null,
        currency: LedgerCurrency? = null
    ) {
        val old = mutableState.value
        if (old.isSaving || old.isLoading || old.loadFailed || old.notFound) return
        mutableState.value = old.copy(
            currency = currency ?: old.currency,
            amount = amount ?: old.amount, type = type ?: old.type,
            category = if (type != null && type != old.type) null else category ?: old.category,
            date = date ?: old.date, description = description ?: old.description,
            errors = ValidationErrors(), saved = false, saveFailed = false,
        )
        persist()
    }

    fun selectWallet(wallet: Wallet?) {
        if (state.value.isSaving) return
        mutableState.value = state.value.copy(
            walletId = wallet?.id,
            currency = wallet?.currency?.let(LedgerCurrency::valueOf) ?: state.value.currency
        )
        persist()
    }

    fun save() {
        val input = mutableState.value
        if (input.isSaving || input.isLoading || input.loadFailed || input.notFound) return
        mutableState.value = input.copy(isSaving = true, saved = false, saveFailed = false)
        viewModelScope.launch {
            try {
                val draft = TransactionDraft(
                    draftId,
                    input.amount,
                    input.type,
                    input.category,
                    input.date,
                    input.description,
                    input.currency.name,
                    input.walletId
                )
                if (editId == null) {
                    addTransaction(draft)
                    draftId = UUID.randomUUID().toString()
                    mutableState.value = AddTransactionState(
                        type = input.type,
                        currency = input.currency,
                        saved = true
                    )
                } else {
                    editTransaction.save(draft)
                    mutableState.value = input.copy(saved = true)
                }
                persist()
            } catch (error: CancellationException) {
                throw error
            } catch (_: TransactionNotFound) {
                mutableState.value = input.copy(notFound = true)
            } catch (error: InvalidTransaction) {
                mutableState.value = input.copy(errors = error.errors)
            } catch (_: Exception) {
                mutableState.value = input.copy(saveFailed = true)
            } finally {
                mutableState.value = mutableState.value.copy(isSaving = false)
            }
        }
    }

    private fun persist() {
        val value = mutableState.value
        savedState["walletId"] = value.walletId
        savedState["currency"] = value.currency.name
        savedState["id"] = draftId
        savedState["amount"] = value.amount
        savedState["type"] = value.type.name
        savedState["category"] = value.category?.name
        savedState["date"] = value.date.toEpochDay()
        savedState["description"] = value.description
    }
}
