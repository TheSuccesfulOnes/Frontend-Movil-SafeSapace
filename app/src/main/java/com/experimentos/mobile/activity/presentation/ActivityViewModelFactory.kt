package com.experimentos.mobile.activity.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.experimentos.mobile.activity.data.ActivityApi

class ActivityViewModelFactory(private val activityApi: ActivityApi) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ActivityViewModel::class.java))
        return ActivityViewModel(activityApi) as T
    }
}
