package com.experimentos.mobile.authentication.domain

import com.experimentos.mobile.authentication.data.AuthApi
import com.experimentos.mobile.authentication.data.LoginRequest
import com.experimentos.mobile.authentication.data.RegisterRequest
import com.experimentos.mobile.authentication.data.PasswordRecoveryRequest
import com.experimentos.mobile.authentication.data.PasswordResetConfirmRequest
import com.experimentos.mobile.shared.data.Session

interface AuthRepository {
    suspend fun register(request: RegisterRequest): Result<Unit>
    suspend fun login(identifier: String, password: String): Result<Session>
    suspend fun requestPasswordRecovery(identifier: String): Result<String>
    suspend fun confirmPasswordRecovery(
        token: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<String>
}

class DefaultAuthRepository(private val api: AuthApi) : AuthRepository {
    override suspend fun register(request: RegisterRequest): Result<Unit> = runCatching {
        check(api.register(request).token.isNotBlank()) { "The registration response did not include a token." }
    }

    override suspend fun login(identifier: String, password: String): Result<Session> = runCatching {
        api.login(LoginRequest(identifier.trim(), password)).let {
            Session(
                userId = it.userId,
                token = it.token,
                username = it.username,
                displayName = it.displayName,
                role = it.role,
            )
        }
    }

    override suspend fun requestPasswordRecovery(identifier: String): Result<String> = runCatching {
        api.requestPasswordRecovery(PasswordRecoveryRequest(identifier.trim())).message
    }

    override suspend fun confirmPasswordRecovery(
        token: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<String> = runCatching {
        api.confirmPasswordRecovery(
            PasswordResetConfirmRequest(token.trim(), newPassword, confirmPassword),
        ).message
    }

}
