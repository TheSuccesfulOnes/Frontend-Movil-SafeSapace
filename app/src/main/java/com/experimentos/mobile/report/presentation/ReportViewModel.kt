package com.experimentos.mobile.report.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimentos.mobile.report.data.CreateReportRequest
import com.experimentos.mobile.report.data.ReportApi
import com.experimentos.mobile.report.data.ReportResponse
import com.experimentos.mobile.report.data.UpdateReportStatusRequest
import com.experimentos.mobile.shared.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportUiState(
    val isLoading: Boolean = false,
    val reports: List<ReportResponse> = emptyList(),
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

class ReportViewModel(
    private val reportApi: ReportApi,
    private val loadAll: Boolean = false,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = mutableState.asStateFlow()

    init {
        if (loadAll) loadReports()
    }

    fun create(request: CreateReportRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { reportApi.create(request) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        message = "Reporte enviado correctamente.",
                    )
                    onSuccess()
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo enviar el reporte."),
                    )
                }
        }
    }

    fun loadReports() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            runCatching { if (loadAll) reportApi.getAll() else reportApi.getMine() }
                .onSuccess { reports ->
                    mutableState.value = mutableState.value.copy(isLoading = false, reports = reports)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = error.toUserMessage("No se pudieron cargar los reportes."),
                    )
                }
        }
    }

    fun updateStatus(id: Long, status: String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { reportApi.updateStatus(id, UpdateReportStatusRequest(status)) }
                .onSuccess { updated ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        reports = mutableState.value.reports.map { report ->
                            if (report.id == updated.id) updated else report
                        },
                        message = "Estado del reporte actualizado.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.toUserMessage("No se pudo actualizar el reporte."),
                    )
                }
        }
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null, errorMessage = null)
    }
}
