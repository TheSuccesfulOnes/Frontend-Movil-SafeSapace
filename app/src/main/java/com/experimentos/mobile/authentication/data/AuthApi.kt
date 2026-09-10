package com.experimentos.mobile.authentication.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("confirm_password")
    val confirmPassword: String,
)

data class LoginRequest(val identifier: String, val password: String)

data class PasswordRecoveryRequest(val identifier: String)

data class PasswordResetConfirmRequest(
    val token: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String,
)

data class PasswordRecoveryResponse(val message: String)

data class AuthResponse(
    val token: String,
    val username: String,
    @SerializedName("display_name")
    val displayName: String,
    val role: String,
    @SerializedName("user_id") val userId: Long?,
)

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/password-recovery/request")
    suspend fun requestPasswordRecovery(
        @Body request: PasswordRecoveryRequest,
    ): PasswordRecoveryResponse

    @POST("api/v1/auth/password-recovery/confirm")
    suspend fun confirmPasswordRecovery(
        @Body request: PasswordResetConfirmRequest,
    ): PasswordRecoveryResponse

}
