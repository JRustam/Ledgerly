package com.jrustam.ledgerly

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.jrustam.ledgerly.data.DataStoreCurrencyPreferences
import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.addtransaction.AddTransactionViewModel
import com.jrustam.ledgerly.ui.settings.SettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun currencyPreferencePersistsAfterDataStoreReopens() = runTest {
        val file = temporary.root.resolve("settings.preferences_pb")
        suspend fun withStore(action: suspend (CurrencyPreferences) -> Unit) {
            val job = SupervisorJob()
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
            try { action(DataStoreCurrencyPreferences(store)) }
            finally { job.cancelAndJoin() }
        }
        withStore { prefs ->
            assertEquals(LedgerCurrency.USD, prefs.defaultCurrency.first())
            prefs.setDefaultCurrency(LedgerCurrency.AZN)
        }
        withStore { prefs -> assertEquals(LedgerCurrency.AZN, prefs.defaultCurrency.first()) }
    }

    @Test fun changingDefaultOnlyAffectsNewDraftsAndDoesNotConvertRecords() = runTest(dispatcher) {
        val preferences = FakeCurrencyPreferences()
        val repository = FakeTransactionRepository()
        val initial = AddTransactionViewModel(AddTransaction(repository), SavedStateHandle(), EditTransaction(repository), preferences)
        advanceUntilIdle()
        initial.edit(amount = "12.34", category = Category.FOOD)
        initial.save(); advanceUntilIdle()
        preferences.setDefaultCurrency(LedgerCurrency.AZN)
        val next = AddTransactionViewModel(AddTransaction(repository), SavedStateHandle(), EditTransaction(repository), preferences)
        advanceUntilIdle()
        assertEquals(LedgerCurrency.AZN, next.state.value.currency)
        assertEquals("USD", repository.rows.value.single().currency)
        assertEquals(1234L, repository.rows.value.single().amountMinor)
        val edit = AddTransactionViewModel(AddTransaction(repository), SavedStateHandle(mapOf("transactionId" to repository.rows.value.single().id)), EditTransaction(repository), preferences)
        advanceUntilIdle()
        assertEquals(LedgerCurrency.USD, edit.state.value.currency)
        edit.edit(currency = LedgerCurrency.EUR)
        edit.save(); advanceUntilIdle()
        assertEquals("EUR", repository.rows.value.single().currency)
        assertEquals(1234L, repository.rows.value.single().amountMinor)
    }

    @Test fun settingsChangesLanguageThroughPersistedLocaleProvider() = runTest(dispatcher) {
        val languages = FakeLanguagePreferences()
        val vm = SettingsViewModel(FakeCurrencyPreferences(), languages)
        advanceUntilIdle()
        AppLanguage.entries.forEach {
            vm.selectLanguage(it)
            assertEquals(it, languages.selectedLanguage())
            assertEquals(it, vm.state.value.language)
        }
    }
}
