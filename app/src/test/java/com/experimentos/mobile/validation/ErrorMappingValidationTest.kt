package com.experimentos.mobile.validation

import com.experimentos.mobile.authentication.presentation.toAuthUserMessage
import com.experimentos.mobile.shared.presentation.toUserMessage
import com.experimentos.mobile.authentication.domain.RegistrationPasswordPolicy
import com.experimentos.mobile.authentication.data.AuthApi
import com.experimentos.mobile.authentication.domain.DefaultAuthRepository
import com.experimentos.mobile.profile.data.ProfileApi
import kotlinx.coroutines.runBlocking
import java.io.IOException
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AuthErrorValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            mapped("username-conflict", 409, "username is already used", "El nombre de usuario ya está utilizado."),
            mapped("email-conflict", 409, "email is already used", "El correo electrónico ya está utilizado."),
            mapped("password-mismatch", 400, "passwords do not match", "Las contraseñas no coinciden."),
            mapped("special-character", 422, "special character", RegistrationPasswordPolicy.REQUIREMENTS_MESSAGE),
            mapped("bcrypt-byte-limit", 400, "password exceeds limit", RegistrationPasswordPolicy.TOO_LONG_MESSAGE),
            mapped("password-minimum", 422, "at least 8 characters", "La contraseña debe tener al menos 8 caracteres."),
            mapped("invalid-credentials-validation", 400, "invalid credentials", "El usuario o la contraseña no son válidos."),
            mapped("expired-reset-token", 400, "invalid or expired reset token", "El enlace de recuperación no es válido o ya expiró. Solicita uno nuevo."),
            mapped("unknown-validation-is-safe", 400, "SECRET database stack", "Revisa los datos ingresados."),
            mapped("unknown-conflict-is-safe", 409, "SECRET database stack", "Revisa los datos ingresados."),
            mapped("unknown-unprocessable-is-safe", 422, "SECRET database stack", "Revisa los datos ingresados."),
            mapped("401-hides-details", 401, "SECRET", "Las credenciales no son válidas."), mapped("403-hides-details", 403, "SECRET", "Las credenciales no son válidas."),
            mapped("500-hides-details", 500, "SECRET", "El servidor no pudo completar la operación."), mapped("503-hides-details", 503, "SECRET", "El servidor no pudo completar la operación."),
            case("network-details-hidden", "unit") { assertEquals("No se pudo conectar con el servidor. Verifica tu conexión.", IOException("SECRET").toAuthUserMessage()) },
            case("runtime-details-hidden", "unit") { assertEquals("No se pudo completar la operación. Intenta nuevamente.", IllegalStateException("SECRET").toAuthUserMessage()) },
            case("json-message-case-insensitive", "unit") { assertEquals("El correo electrónico ya está utilizado.", httpError(409, "{\"message\":\"EMAIL IS ALREADY used\"}").toAuthUserMessage()) },
            case("json-message-overrides-unrelated-body", "unit") { assertEquals("Revisa los datos ingresados.", httpError(400, "{\"debug\":\"email is already used\",\"message\":\"other\"}").toAuthUserMessage()) },
            case("http-repository-to-auth-message") { localHttp("{\"message\":\"invalid or expired reset token\"}", 400) { r, _ -> runBlocking { val result = DefaultAuthRepository(r.create(AuthApi::class.java)).confirmPasswordRecovery("token", "abcdefgh", "abcdefgh"); assertTrue(result.exceptionOrNull()!!.toAuthUserMessage().contains("expiró")) } } },
        )
        private fun mapped(name: String, code: Int, body: String, expected: String) = case(name, "unit") { assertEquals(expected, httpError(code, body).toAuthUserMessage()) }
    }
}

@RunWith(Parameterized::class)
class UserErrorValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            mapped("400-username", 400, "username exists", "El nombre de usuario ya está utilizado."), mapped("409-username", 409, "username exists", "El nombre de usuario ya está utilizado."),
            mapped("400-email", 400, "email exists", "El correo electrónico ya está utilizado."), mapped("409-email", 409, "email exists", "El correo electrónico ya está utilizado."),
            mapped("already-answered", 409, "already been answered", "Ya respondiste esta encuesta."), mapped("survey-closed", 400, "survey is not open", "Esta encuesta ya no está disponible."),
            mapped("400-unknown", 400, "SECRET", "Revisa los datos ingresados."), mapped("409-unknown", 409, "SECRET", "La operación entra en conflicto con un registro existente."),
            mapped("401-session-invalid", 401, "SECRET", "Tu sesión ya no es válida. Inicia sesión nuevamente."), mapped("403-permission", 403, "SECRET", "No tienes permisos para realizar esta acción."),
            mapped("404-not-found", 404, "SECRET", "No se encontró la información solicitada."), mapped("503-assistant-unavailable", 503, "SECRET", "El asistente no está disponible en este momento. Intenta nuevamente."),
            mapped("500-caller-fallback", 500, "SECRET", "safe fallback"), mapped("422-caller-fallback", 422, "SECRET", "safe fallback"),
            case("io-details-hidden", "unit") { assertEquals("No se pudo conectar con el servidor.", IOException("SECRET").toUserMessage("safe fallback")) },
            case("runtime-caller-fallback", "unit") { assertEquals("safe fallback", IllegalStateException("SECRET").toUserMessage("safe fallback")) },
            case("json-field-case-insensitive", "unit") { assertEquals("El correo electrónico ya está utilizado.", httpError(409, "{\"message\":\"EMAIL EXISTS\"}").toUserMessage("safe fallback")) },
            case("json-selected-message-not-debug", "unit") { assertEquals("Revisa los datos ingresados.", httpError(400, "{\"debug\":\"username\",\"message\":\"other\"}").toUserMessage("safe fallback")) },
            case("empty-error-body-fallback", "unit") { assertEquals("Revisa los datos ingresados.", httpError(400, "").toUserMessage("safe fallback")) },
            case("http-profile-error-to-user-message") { localHttp("{\"message\":\"email already exists\"}", 409) { r, _ -> runBlocking { val result = runCatching { r.create(ProfileApi::class.java).getProfile() }; assertEquals("El correo electrónico ya está utilizado.", result.exceptionOrNull()!!.toUserMessage("safe fallback")) } } },
        )
        private fun mapped(name: String, code: Int, body: String, expected: String) = case(name, "unit") { assertEquals(expected, httpError(code, body).toUserMessage("safe fallback")) }
    }
}
