package com.experimentos.mobile.profile.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

data class ProfileResponse(
    @SerializedName("user_id") val userId: Long,
    val username: String,
    val email: String?,
    @SerializedName("display_name") val displayName: String,
    val role: String,
    val language: String,
    val theme: String,
)

data class UpdateAccountRequest(
    val username: String,
    val email: String?,
    @SerializedName("display_name") val displayName: String,
)

data class UpdateAccountResponse(
    val profile: ProfileResponse,
    val token: String,
)

data class UpdatePreferencesRequest(val language: String, val theme: String)

interface ProfileApi {
    @GET("api/v1/profile")
    suspend fun getProfile(): ProfileResponse

    @PUT("api/v1/profile/account")
    suspend fun updateAccount(@Body request: UpdateAccountRequest): UpdateAccountResponse

    @PUT("api/v1/profile/preferences")
    suspend fun updatePreferences(@Body request: UpdatePreferencesRequest): ProfileResponse
}
