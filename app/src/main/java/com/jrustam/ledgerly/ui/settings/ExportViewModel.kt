package com.jrustam.ledgerly.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrustam.ledgerly.data.local.CsvDocumentWriter
import com.jrustam.ledgerly.domain.usecase.ExportTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExportState {
    data object Idle : ExportState
    data object Writing : ExportState
    data object Success : ExportState
    data object Failed : ExportState
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportTransactions: ExportTransactionsUseCase,
    private val writer: CsvDocumentWriter,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ExportState>(ExportState.Idle)
    val state = mutableState.asStateFlow()

    fun export(destination: Uri) {
        if (state.value == ExportState.Writing) return
        mutableState.value = ExportState.Writing
        viewModelScope.launch {
            try {
                writer.write(destination, exportTransactions())
                mutableState.value = ExportState.Success
            } catch (error: CancellationException) {
                mutableState.value = ExportState.Idle
                throw error
            } catch (_: Exception) {
                mutableState.value = ExportState.Failed
            }
        }
    }
}
