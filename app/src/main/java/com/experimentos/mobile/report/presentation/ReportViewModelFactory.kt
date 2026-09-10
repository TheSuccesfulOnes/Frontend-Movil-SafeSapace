package com.experimentos.mobile.report.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.experimentos.mobile.report.data.ReportApi

class ReportViewModelFactory(
    private val reportApi: ReportApi,
    private val loadAll: Boolean = false,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReportViewModel::class.java))
        return ReportViewModel(reportApi, loadAll) as T
    }
}
