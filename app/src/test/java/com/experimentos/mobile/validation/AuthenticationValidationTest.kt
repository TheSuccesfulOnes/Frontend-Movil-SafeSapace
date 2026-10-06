@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.experimentos.mobile.validation

import com.experimentos.mobile.authentication.domain.RegistrationPasswordPolicy as Policy
import com.experimentos.mobile.authentication.data.*
import com.experimentos.mobile.authentication.presentation.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AuthViewModelValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("login-empty-identifier", "unit") { val vm = AuthViewModel(repository, sessions); vm.login("", "p"); assertNotNull(vm.state.value.errorMessage); assertTrue(auth.calls.isEmpty()) },
            case("login-blank-password", "unit") { val vm = AuthViewModel(repository, sessions); vm.login("ana", " \t"); assertNotNull(vm.state.value.errorMessage); assertTrue(auth.calls.isEmpty()) },
            case("login-success-persists-session") { auth.give("login", authResponse); val vm = AuthViewModel(repository, sessions); vm.login(" ana ", " secret "); drain(); assertEquals(session, sessions.session.value); assertEquals(LoginRequest("ana", " secret "), auth.args("login")[0]); assertFalse(vm.state.value.isLoading) },
            case("login-network-failure-no-session") { auth.fail("login"); val vm = AuthViewModel(repository, sessions); vm.login("ana", "p"); drain(); assertNull(sessions.currentToken()); assertTrue(vm.state.value.errorMessage!!.contains("conexión")); assertFalse(vm.state.value.isLoading) },
            case("login-invalid-credentials") { auth.fail("login", httpError(401)); val vm = AuthViewModel(repository, sessions); vm.login("ana", "p"); drain(); assertTrue(vm.state.value.errorMessage!!.contains("credenciales")); assertNull(sessions.currentToken()) },
            case("login-pending-state") { auth.hold("login"); val vm = AuthViewModel(repository, sessions); vm.login("ana", "p"); drain(); assertTrue(vm.state.value.isLoading); auth.release("login", authResponse); drain(); assertFalse(vm.state.value.isLoading) },
            case("register-empty-name", "unit") { rejectedRegistration("", "ana", "a@b.test", "Abcdef1!", "Abcdef1!") },
            case("register-empty-username", "unit") { rejectedRegistration("Ana", " ", "a@b.test", "Abcdef1!", "Abcdef1!") },
            case("register-empty-email", "unit") { rejectedRegistration("Ana", "ana", "\n", "Abcdef1!", "Abcdef1!") },
            case("register-empty-password", "unit") { rejectedRegistration("Ana", "ana", "a@b.test", "", "x") },
            case("register-empty-confirmation", "unit") { rejectedRegistration("Ana", "ana", "a@b.test", "Abcdef1!", " ") },
            case("register-name-below-minimum", "unit") { rejectedRegistration(" A ", "ana", "a@b.test", "Abcdef1!", "Abcdef1!") },
            case("register-name-above-maximum", "unit") { rejectedRegistration("A".repeat(101), "ana", "a@b.test", "Abcdef1!", "Abcdef1!") },
            case("register-password-mismatch", "unit") { rejectedRegistration("Ana", "ana", "a@b.test", "Abcdef1!", "Abcdef2!") },
            case("register-password-requirements", "unit") { rejectedRegistration("Ana", "ana", "a@b.test", "abcdefgh", "abcdefgh") },
            case("register-bcrypt-byte-limit", "unit") { val p = "Ab1!" + "é".repeat(35); rejectedRegistration("Ana", "ana", "a@b.test", p, p) },
            case("register-minimum-name-and-normalization") { auth.give("register", authResponse); val vm = AuthViewModel(repository, sessions); vm.register(" Al ", " ana ", " a@b.test ", "Abcdef1!", "Abcdef1!"); drain(); assertEquals(RegisterRequest("ana", "a@b.test", "Abcdef1!", "Abcdef1!", "Al"), auth.args("register")[0]); assertTrue(vm.state.value.registrationCompleted); assertNull(sessions.currentToken()) },
            case("register-maximum-name-and-72-bytes") { auth.give("register", authResponse); val p = "Ab1!" + "a".repeat(68); val vm = AuthViewModel(repository, sessions); vm.register("A".repeat(100), "ana", "a@b.test", p, p); drain(); assertTrue(vm.state.value.registrationCompleted) },
            case("register-busy-guard") { auth.hold("register"); val vm = AuthViewModel(repository, sessions); vm.register("Ana", "ana", "a@b.test", "Abcdef1!", "Abcdef1!"); drain(); vm.register("Ana", "ana", "a@b.test", "Abcdef1!", "Abcdef1!"); drain(); assertEquals(1, auth.count("register")); auth.release("register", authResponse); drain() },
            case("register-conflict-clear-message") { auth.fail("register", httpError(409, "{\"message\":\"username is already used\"}")); val vm = AuthViewModel(repository, sessions); vm.register("Ana", "ana", "a@b.test", "Abcdef1!", "Abcdef1!"); drain(); assertTrue(vm.state.value.errorMessage!!.contains("usuario")); assertFalse(vm.state.value.registrationCompleted); vm.clearMessage(); assertNull(vm.state.value.errorMessage) },
        )
    }
}

private fun TestEnv.rejectedRegistration(name: String, username: String, email: String, password: String, confirmation: String) {
    val vm = AuthViewModel(repository, sessions)
    vm.register(name, username, email, password, confirmation)
    drain()
    assertNotNull(vm.state.value.errorMessage)
    assertFalse(vm.state.value.registrationCompleted)
    assertTrue(auth.calls.isEmpty())
}

@RunWith(Parameterized::class)
class RecoveryValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("request-empty", "unit") { val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery(""); assertNotNull(vm.state.value.errorMessage); assertTrue(auth.calls.isEmpty()) },
            case("request-whitespace", "unit") { val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery("\t\n"); assertNotNull(vm.state.value.errorMessage); assertTrue(auth.calls.isEmpty()) },
            case("request-username-normalized") { auth.give("requestPasswordRecovery", PasswordRecoveryResponse("sent")); val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery(" ana "); drain(); assertEquals(PasswordRecoveryRequest("ana"), auth.args("requestPasswordRecovery")[0]); assertTrue(vm.state.value.requestSent) },
            case("request-email-success") { auth.give("requestPasswordRecovery", PasswordRecoveryResponse("sent")); val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery("a@example.test"); drain(); assertTrue(vm.state.value.requestSent); assertFalse(vm.state.value.completed) },
            case("request-loading") { auth.hold("requestPasswordRecovery"); val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery("ana"); drain(); assertTrue(vm.state.value.isLoading); auth.release("requestPasswordRecovery", PasswordRecoveryResponse("sent")); drain(); assertFalse(vm.state.value.isLoading) },
            case("request-network-error") { auth.fail("requestPasswordRecovery"); val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery("ana"); drain(); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.requestSent) },
            case("request-server-details-hidden") { auth.fail("requestPasswordRecovery", httpError(500, "private stack trace")); val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery("ana"); drain(); assertFalse(vm.state.value.errorMessage!!.contains("private")) },
            case("confirm-empty-token", "unit") { rejectedRecovery("", "abcdefgh", "abcdefgh") },
            case("confirm-blank-token", "unit") { rejectedRecovery(" \n", "abcdefgh", "abcdefgh") },
            case("confirm-empty-password", "unit") { rejectedRecovery("token", "", "abcdefgh") },
            case("confirm-empty-confirmation", "unit") { rejectedRecovery("token", "abcdefgh", "") },
            case("confirm-mismatch", "unit") { rejectedRecovery("token", "abcdefgh", "abcdefgi") },
            case("confirm-seven-characters", "unit") { rejectedRecovery("token", "abcdefg", "abcdefg") },
            case("confirm-eight-characters") { auth.give("confirmPasswordRecovery", PasswordRecoveryResponse("reset")); val vm = PasswordRecoveryViewModel(repository); vm.confirmRecovery(" token ", "abcdefgh", "abcdefgh"); drain(); assertEquals(PasswordResetConfirmRequest("token", "abcdefgh", "abcdefgh"), auth.args("confirmPasswordRecovery")[0]); assertTrue(vm.state.value.completed) },
            case("confirm-password-not-trimmed") { auth.give("confirmPasswordRecovery", PasswordRecoveryResponse("reset")); val vm = PasswordRecoveryViewModel(repository); vm.confirmRecovery("token", " secret ", " secret "); drain(); assertEquals(" secret ", (auth.args("confirmPasswordRecovery")[0] as PasswordResetConfirmRequest).newPassword) },
            case("confirm-loading") { auth.hold("confirmPasswordRecovery"); val vm = PasswordRecoveryViewModel(repository); vm.confirmRecovery("token", "abcdefgh", "abcdefgh"); drain(); assertTrue(vm.state.value.isLoading); auth.release("confirmPasswordRecovery", PasswordRecoveryResponse("reset")); drain(); assertTrue(vm.state.value.completed) },
            case("confirm-expired-token") { auth.fail("confirmPasswordRecovery", httpError(400, "invalid or expired reset token")); val vm = PasswordRecoveryViewModel(repository); vm.confirmRecovery("token", "abcdefgh", "abcdefgh"); drain(); assertTrue(vm.state.value.errorMessage!!.contains("expiró")); assertFalse(vm.state.value.completed) },
            case("confirm-network-failure-retains-request") { auth.give("requestPasswordRecovery", PasswordRecoveryResponse("sent")); auth.fail("confirmPasswordRecovery"); val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery("ana"); drain(); vm.confirmRecovery("token", "abcdefgh", "abcdefgh"); drain(); assertTrue(vm.state.value.requestSent); assertFalse(vm.state.value.isLoading) },
            case("confirm-retry-after-failure") { auth.fail("confirmPasswordRecovery"); auth.give("confirmPasswordRecovery", PasswordRecoveryResponse("reset")); val vm = PasswordRecoveryViewModel(repository); vm.confirmRecovery("token", "abcdefgh", "abcdefgh"); drain(); vm.confirmRecovery("token", "abcdefgh", "abcdefgh"); drain(); assertTrue(vm.state.value.completed); assertNull(vm.state.value.errorMessage) },
            case("confirm-invalid-input-retains-request") { auth.give("requestPasswordRecovery", PasswordRecoveryResponse("sent")); val vm = PasswordRecoveryViewModel(repository); vm.requestRecovery("ana"); drain(); vm.confirmRecovery("", "abcdefgh", "abcdefgh"); assertTrue(vm.state.value.requestSent); assertEquals(0, auth.count("confirmPasswordRecovery")) },
        )
    }
}
private fun TestEnv.rejectedRecovery(token: String, password: String, confirmation: String) {
    val vm = PasswordRecoveryViewModel(repository)
    vm.confirmRecovery(token, password, confirmation)
    drain(); assertNotNull(vm.state.value.errorMessage); assertTrue(auth.calls.isEmpty()); assertFalse(vm.state.value.completed)
}

@RunWith(Parameterized::class)
class PasswordBoundaryTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            passwordCase("empty", "", false), passwordCase("seven-codepoints", "Abcd1!x", false),
            passwordCase("eight-codepoints", "Abcde1!x", true), passwordCase("no-upper", "abcdef1!", false),
            passwordCase("no-lower", "ABCDEF1!", false), passwordCase("no-digit", "Abcdefg!", false),
            passwordCase("no-symbol", "Abcdef12", false), passwordCase("space-not-symbol", "Abcdef1 ", false),
            passwordCase("control-not-symbol", "Abcdef1\u0001", false), passwordCase("nbsp-not-symbol", "Abcdef1\u00a0", false),
            passwordCase("unicode-upper", "Ábcdef1!", true), passwordCase("unicode-digit", "Abcdef١!", true),
            passwordCase("emoji-symbol", "Abcdef1🔒", true), passwordCase("72-bytes", "Ab1!" + "a".repeat(68), true),
            passwordCase("73-bytes", "Ab1!" + "a".repeat(69), false), passwordCase("multibyte-72", "Ab1!" + "é".repeat(34), true),
            passwordCase("surrogate-count-not-length", "A1!🔒🔒🔒a", false),
        )
        private fun passwordCase(name: String, password: String, valid: Boolean) = case(name, "unit") {
            assertEquals(valid, Policy.validationError(password) == null)
            if (password.toByteArray(Charsets.UTF_8).size > 72) assertEquals(Policy.TOO_LONG_MESSAGE, Policy.validationError(password))
        }
    }
}
