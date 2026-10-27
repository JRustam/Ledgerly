package com.jrustam.ledgerly

import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.model.Appearance
import com.jrustam.ledgerly.domain.model.ExchangeQuote
import com.jrustam.ledgerly.domain.model.ProductPreferences
import com.jrustam.ledgerly.domain.repository.ExchangeRateProvider
import com.jrustam.ledgerly.domain.repository.ProductPreferencesRepository
import com.jrustam.ledgerly.domain.usecase.ConvertCurrencyUseCase
import com.jrustam.ledgerly.ui.finance.ProductViewModel
import java.math.BigDecimal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModels = mutableListOf<ProductViewModel>()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        try { runTest(dispatcher) { viewModels.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } } }
        finally { Dispatchers.resetMain() }
    }
    private fun model(provider: ExchangeRateProvider) = ProductViewModel(object : ProductPreferencesRepository {
        override val data = flowOf(ProductPreferences())
        override suspend fun appearance(value: Appearance) = Unit
        override suspend fun completeOnboarding() = Unit
    }, provider, ConvertCurrencyUseCase()).also(viewModels::add)

    @Test fun cancelledRequestCannotClearReplacementLoadingState() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val vm = model(object : ExchangeRateProvider {
            override suspend fun quote(base: String, target: String): ExchangeQuote? { release.await(); return null }
            override suspend fun refresh(base: String, target: String): ExchangeQuote = error("Not used")
        })
        vm.loadRate("USD", "EUR"); runCurrent()
        vm.loadRate("USD", "UAH"); runCurrent()
        assertTrue(vm.rateBusy.value)
        release.complete(Unit); runCurrent()
        assertFalse(vm.rateBusy.value)
        assertNull(vm.quote.value)
        assertFalse(vm.rateError.value)
    }

    @Test fun failedRefreshRetainsCachedQuoteAndReportsFailure() = runTest(dispatcher) {
        val cached = ExchangeQuote("USD", "EUR", BigDecimal("0.9"), 123, "2026-10-24")
        val vm = model(object : ExchangeRateProvider {
            override suspend fun quote(base: String, target: String) = cached
            override suspend fun refresh(base: String, target: String): ExchangeQuote = error("Offline")
        })
        vm.loadRate("USD", "EUR", true); runCurrent()
        assertEquals(cached, vm.quote.value)
        assertTrue(vm.rateError.value)
        assertFalse(vm.rateBusy.value)
    }
}
