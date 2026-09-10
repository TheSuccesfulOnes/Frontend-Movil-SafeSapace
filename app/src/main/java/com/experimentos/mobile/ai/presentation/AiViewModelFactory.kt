package com.experimentos.mobile.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.experimentos.mobile.ai.data.AiApi

class AiViewModelFactory(private val aiApi: AiApi) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AiViewModel::class.java))
        return AiViewModel(aiApi) as T
    }
}
