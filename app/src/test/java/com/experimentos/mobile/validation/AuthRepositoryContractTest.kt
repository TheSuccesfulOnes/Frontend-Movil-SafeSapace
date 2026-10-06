package com.experimentos.mobile.validation

import com.experimentos.mobile.authentication.data.*
import com.experimentos.mobile.authentication.domain.DefaultAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AuthRepositoryContractTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("login-wire-fields") { localHttp(AUTH_JSON) { retrofit, server -> runBlocking { val result = DefaultAuthRepository(retrofit.create(AuthApi::class.java)).login(" ana ", " secret "); assertEquals(session, result.getOrThrow()); val json = requestJson(server); assertEquals("ana", json["identifier"].asString); assertEquals(" secret ", json["password"].asString); assertEquals(setOf("identifier", "password"), json.keySet()) } } },
            case("login-endpoint") { localHttp(AUTH_JSON) { r, s -> runBlocking { DefaultAuthRepository(r.create(AuthApi::class.java)).login("ana", "p").getOrThrow(); val req = s.takeRequest(); assertEquals("POST", req.method); assertEquals("/api/v1/auth/login", req.path) } } },
            case("login-immutable-user-id") { localHttp(AUTH_JSON.replace("7", "9007199254740991")) { r, _ -> runBlocking { assertEquals(9007199254740991L, DefaultAuthRepository(r.create(AuthApi::class.java)).login("ana", "p").getOrThrow().userId) } } },
            case("login-legacy-null-id") { localHttp(AUTH_JSON.replace("\"user_id\":7", "\"user_id\":null")) { r, _ -> runBlocking { assertNull(DefaultAuthRepository(r.create(AuthApi::class.java)).login("ana", "p").getOrThrow().userId) } } },
            case("login-role-preserved-for-navigation-guard") { localHttp(AUTH_JSON.replace("EMPLOYEE", "SYSTEM_ADMIN")) { r, _ -> runBlocking { assertEquals("SYSTEM_ADMIN", DefaultAuthRepository(r.create(AuthApi::class.java)).login("ana", "p").getOrThrow().role) } } },
            case("login-401-result-failure") { localHttp("{}", 401) { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).login("ana", "p").isFailure) } } },
            case("login-malformed-json-failure") { localHttp("{") { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).login("ana", "p").isFailure) } } },
            case("register-wire-snake-case") { localHttp(AUTH_JSON) { r, s -> runBlocking { DefaultAuthRepository(r.create(AuthApi::class.java)).register(registration).getOrThrow(); val json = requestJson(s); assertEquals("Ana", json["display_name"].asString); assertEquals("Abcdef1!", json["confirm_password"].asString); assertFalse(json.has("confirmPassword")); assertFalse(json.has("displayName")) } } },
            case("register-endpoint-and-password-preservation") { localHttp(AUTH_JSON) { r, s -> runBlocking { DefaultAuthRepository(r.create(AuthApi::class.java)).register(registration.copy(password = " secret ")).getOrThrow(); val req = s.takeRequest(); assertEquals("/api/v1/auth/register", req.path); assertTrue(req.body.readUtf8().contains(" secret ")) } } },
            case("register-empty-response-token-rejected") { localHttp(AUTH_JSON.replace("local-token", "")) { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).register(registration).isFailure) } } },
            case("register-whitespace-response-token-rejected") { localHttp(AUTH_JSON.replace("local-token", "   ")) { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).register(registration).isFailure) } } },
            case("register-conflict-failure") { localHttp("{}", 409) { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).register(registration).isFailure) } } },
            case("request-recovery-identifier-trim") { localHttp("{\"message\":\"sent\"}") { r, s -> runBlocking { assertEquals("sent", DefaultAuthRepository(r.create(AuthApi::class.java)).requestPasswordRecovery(" ana ").getOrThrow()); assertEquals("ana", requestJson(s)["identifier"].asString) } } },
            case("request-recovery-endpoint") { localHttp("{\"message\":\"sent\"}") { r, s -> runBlocking { DefaultAuthRepository(r.create(AuthApi::class.java)).requestPasswordRecovery("ana").getOrThrow(); assertEquals("/api/v1/auth/password-recovery/request", s.takeRequest().path) } } },
            case("request-recovery-server-failure") { localHttp("{}", 503) { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).requestPasswordRecovery("ana").isFailure) } } },
            case("request-recovery-unicode-identifier") { localHttp("{\"message\":\"sent\"}") { r, s -> runBlocking { DefaultAuthRepository(r.create(AuthApi::class.java)).requestPasswordRecovery(" árbol ").getOrThrow(); assertEquals("árbol", requestJson(s)["identifier"].asString) } } },
            case("confirm-token-trim-password-preserved") { localHttp("{\"message\":\"reset\"}") { r, s -> runBlocking { assertEquals("reset", DefaultAuthRepository(r.create(AuthApi::class.java)).confirmPasswordRecovery(" token ", " secret ", " secret ").getOrThrow()); val json = requestJson(s); assertEquals("token", json["token"].asString); assertEquals(" secret ", json["new_password"].asString); assertEquals(" secret ", json["confirm_password"].asString) } } },
            case("confirm-endpoint-and-schema") { localHttp("{\"message\":\"reset\"}") { r, s -> runBlocking { DefaultAuthRepository(r.create(AuthApi::class.java)).confirmPasswordRecovery("t", "p", "p").getOrThrow(); val req = s.takeRequest(); assertEquals("/api/v1/auth/password-recovery/confirm", req.path); assertEquals("POST", req.method); assertFalse(req.body.readUtf8().contains("newPassword")) } } },
            case("confirm-expired-token-failure") { localHttp("{}", 400) { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).confirmPasswordRecovery("t", "p", "p").isFailure) } } },
            case("confirm-malformed-response-failure") { localHttp("{") { r, _ -> runBlocking { assertTrue(DefaultAuthRepository(r.create(AuthApi::class.java)).confirmPasswordRecovery("t", "p", "p").isFailure) } } },
        )
        private val registration = RegisterRequest("ana", "ana@example.test", "Abcdef1!", "Abcdef1!", "Ana")
    }
}
