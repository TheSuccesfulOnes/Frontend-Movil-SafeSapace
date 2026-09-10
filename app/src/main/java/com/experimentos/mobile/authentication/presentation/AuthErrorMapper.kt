package com.experimentos.mobile.authentication.presentation

import java.io.IOException
import retrofit2.HttpException

/** Converts authentication failures into safe, actionable messages for the UI. */
internal fun Throwable.toAuthUserMessage(): String = when (this) {
    is IOException -> "No se pudo conectar con el servidor. Verifica tu conexión."
    is HttpException -> when (code()) {
        400, 422 -> apiValidationMessage()
        401, 403 -> "Las credenciales no son válidas."
        409 -> apiValidationMessage()
        else -> "El servidor no pudo completar la operación."
    }
    else -> "No se pudo completar la operación. Intenta nuevamente."
}

/** Maps known API validation messages without exposing raw server details to the UI. */
private fun HttpException.apiValidationMessage(): String {
    val body = response()?.errorBody()?.string().orEmpty().lowercase()
    val message = Regex("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]+)").find(body)?.groupValues?.getOrNull(1)
        ?: body
    return when {
        message.contains("username is already") -> "El nombre de usuario ya está utilizado."
        message.contains("email is already") -> "El correo electrónico ya está utilizado."
        message.contains("passwords do not match") -> "Las contraseñas no coinciden."
        message.contains("at least 8 characters") -> "La contraseña debe tener al menos 8 caracteres."
        message.contains("invalid credentials") -> "El usuario o la contraseña no son válidos."
        message.contains("invalid or expired reset token") ->
            "El enlace de recuperación no es válido o ya expiró. Solicita uno nuevo."
        else -> "Revisa los datos ingresados."
    }
}
