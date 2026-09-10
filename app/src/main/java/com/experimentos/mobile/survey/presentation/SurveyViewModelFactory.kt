package com.experimentos.mobile.survey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.experimentos.mobile.comment.data.CommentApi
import com.experimentos.mobile.survey.data.SurveyApi

class SurveyViewModelFactory(
    private val surveyApi: SurveyApi,
    private val commentApi: CommentApi,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SurveyViewModel::class.java))
        return SurveyViewModel(surveyApi, commentApi) as T
    }
}
