package com.experimentos.mobile.humanresources.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.experimentos.mobile.activity.data.ActivityApi
import com.experimentos.mobile.comment.data.CommentApi
import com.experimentos.mobile.mood.data.MoodApi
import com.experimentos.mobile.survey.data.SurveyApi

class HrHomeViewModelFactory(private val moodApi: MoodApi) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HrHomeViewModel::class.java))
        return HrHomeViewModel(moodApi) as T
    }
}

class HrContentViewModelFactory(
    private val surveyApi: SurveyApi,
    private val activityApi: ActivityApi,
    private val commentApi: CommentApi,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HrContentViewModel::class.java))
        return HrContentViewModel(surveyApi, activityApi, commentApi) as T
    }
}
