package com.jrustam.ledgerly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val currency: LedgerCurrency = LedgerCurrency.USD,
    val language: AppLanguage = AppLanguage.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val currencies: CurrencyPreferences,
    private val languages: LanguagePreferences,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState(language = languages.selectedLanguage()))
    val state = mutableState.asStateFlow()
    private var observer: Job? = null
    init { retry() }
    fun retry() {
        observer?.cancel()
        mutableState.update { it.copy(loading = true, failed = false) }
        observer = viewModelScope.launch {
            currencies.defaultCurrency.catch { error ->
                if (error is CancellationException) throw error
                mutableState.update { it.copy(loading = false, failed = true) }
            }.collect { value -> mutableState.update { it.copy(currency = value, loading = false, failed = false) } }
        }
    }
    fun refreshLanguage() { mutableState.update { it.copy(language = languages.selectedLanguage()) } }
    fun selectLanguage(language: AppLanguage) {
        languages.setLanguage(language)
        mutableState.update { it.copy(language = language) }
    }
    fun selectCurrency(currency: LedgerCurrency) {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try { currencies.setDefaultCurrency(currency) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.update { it.copy(saveFailed = true) } }
            finally { mutableState.update { it.copy(saving = false) } }
        }
    }
}
