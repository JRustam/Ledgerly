package com.jrustam.ledgerly.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state = mutableState.asStateFlow()
    private var observer: Job? = null

    init {
        load()
    }

    fun load() {
        observer?.cancel()
        mutableState.update { it.copy(loading = true, loadFailed = false) }
        observer = viewModelScope.launch {
            try {
                repository.profile.collect { profile ->
                    mutableState.update {
                        it.copy(
                            profile = profile,
                            name = savedState["profileDraft"] ?: profile.displayName,
                            loading = false,
                            loadFailed = false
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun editName(value: String) {
        if (state.value.busy) return
        savedState["profileDraft"] = value
        mutableState.update {
            it.copy(
                name = value,
                nameError = false,
                saved = false,
                saveFailed = false
            )
        }
    }

    fun saveName() {
        val name = state.value.name.trim()
        if (name.isEmpty() || name.length > 60) {
            mutableState.update { it.copy(nameError = true) }
            return
        }
        write {
            repository.saveName(name); savedState["profileDraft"] = name
            mutableState.update { it.copy(name = name) }
        }
    }

    fun photo(source: String?) = write { repository.setPhoto(source) }

    private fun write(action: suspend () -> Unit) {
        if (state.value.busy || state.value.loading || state.value.loadFailed) return
        mutableState.update { it.copy(busy = true, saved = false, saveFailed = false) }
        viewModelScope.launch {
            try {
                action(); mutableState.update { it.copy(saved = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(saveFailed = true) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
