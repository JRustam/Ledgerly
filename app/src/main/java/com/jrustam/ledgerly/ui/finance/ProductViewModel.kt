package com.jrustam.ledgerly.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.model.Appearance
import com.jrustam.ledgerly.domain.model.CurrencyConversion
import com.jrustam.ledgerly.domain.model.ExchangeQuote
import com.jrustam.ledgerly.domain.model.ProductPreferences
import com.jrustam.ledgerly.domain.repository.ExchangeRateProvider
import com.jrustam.ledgerly.domain.repository.ProductPreferencesRepository
import com.jrustam.ledgerly.domain.usecase.ConvertCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@HiltViewModel
class ProductViewModel @Inject constructor(private val preferences: ProductPreferencesRepository,
    private val rates: ExchangeRateProvider,
    private val convertCurrency: ConvertCurrencyUseCase) : ViewModel() {
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    val preferencesState = preferences.data.map<ProductPreferences, ProductPreferences?> { it }
        .catch { if (it is CancellationException) throw it; mutableError.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val mutableQuote = MutableStateFlow<ExchangeQuote?>(null)
    val quote = mutableQuote.asStateFlow()
    private val mutableRateBusy = MutableStateFlow(false)
    val rateBusy = mutableRateBusy.asStateFlow()
    private val mutableRateError = MutableStateFlow(false)
    val rateError = mutableRateError.asStateFlow()
    private var rateJob: Job? = null
    private var rateRequest = 0L
    fun appearance(value: Appearance) = write { preferences.appearance(value) }
    fun finish() = write { preferences.completeOnboarding() }
    private fun write(action: suspend () -> Unit) { viewModelScope.launch {
        try { mutableError.value = false; action() } catch (e: CancellationException) { throw e } catch (_: Exception) { mutableError.value = true }
    } }
    fun convert(amount: String, base: String, target: String, quote: ExchangeQuote?): CurrencyConversion =
        convertCurrency(amount, base, target, quote)

    fun loadRate(base: String, target: String, refresh: Boolean = false) {
        val request = ++rateRequest
        rateJob?.cancel()
        mutableQuote.value = null
        mutableRateError.value = false
        mutableRateBusy.value = true
        rateJob = viewModelScope.launch {
            try {
                val cached = rates.quote(base, target)
                if (request == rateRequest) mutableQuote.value = cached
                if (refresh) {
                    val updated = rates.refresh(base, target)
                    if (request == rateRequest) mutableQuote.value = updated
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (request == rateRequest) mutableRateError.value = true
            } finally {
                // A cancelled request must not clear its successor's loading state.
                if (request == rateRequest) mutableRateBusy.value = false
            }
        }
    }
}
