package com.experimentos.mobile.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.experimentos.mobile.profile.data.ProfileApi
import com.experimentos.mobile.shared.data.AppearanceStore
import com.experimentos.mobile.shared.data.SessionStore

class ProfileViewModelFactory(
    private val profileApi: ProfileApi,
    private val appearanceStore: AppearanceStore,
    private val sessionStore: SessionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return ProfileViewModel(profileApi, appearanceStore, sessionStore) as T
    }
}
