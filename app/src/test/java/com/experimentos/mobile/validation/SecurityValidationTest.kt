package com.experimentos.mobile.validation

import com.experimentos.mobile.shared.navigation.isMobileRoleAllowed
import com.experimentos.mobile.shared.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MobileRoleValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            role("employee-allowed", "EMPLOYEE", true), role("hr-allowed", "HR_MEMBER", true), role("system-admin-denied", "SYSTEM_ADMIN", false),
            role("unknown-role-denied", "MANAGER", false), role("empty-role-denied", "", false), role("blank-role-denied", " \t", false),
            role("lowercase-employee-denied", "employee", false), role("lowercase-hr-denied", "hr_member", false),
            role("leading-space-denied", " EMPLOYEE", false), role("trailing-space-denied", "HR_MEMBER ", false),
            role("newline-denied", "EMPLOYEE\n", false), role("null-string-denied", "null", false),
            role("multiple-roles-denied", "EMPLOYEE,HR_MEMBER", false), role("role-prefix-denied", "EMPLOYEE_ADMIN", false),
            role("unicode-confusable-denied", "ЕMPLOYEE", false), role("zero-width-suffix-denied", "EMPLOYEE\u200b", false),
            persistedRole("persisted-employee-policy", "EMPLOYEE", true), persistedRole("persisted-hr-policy", "HR_MEMBER", true),
            persistedRole("persisted-admin-policy-clears-session", "SYSTEM_ADMIN", false), persistedRole("persisted-unknown-policy-clears-session", "MANAGER", false),
        )
        private fun role(name: String, input: String, expected: Boolean) = case(name, "unit") { assertEquals(expected, isMobileRoleAllowed(input)) }
        private fun persistedRole(name: String, role: String, allowed: Boolean) = case(name) {
            runBlocking { sessions.save(session.copy(role = role)) }
            val restored = SessionStore(prefs)
            assertEquals(allowed, isMobileRoleAllowed(restored.session.value!!.role))
            // Local policy + persistence contract; actual LaunchedEffect/navigation needs a device.
            if (!isMobileRoleAllowed(restored.session.value!!.role)) restored.clearImmediately()
            assertEquals(allowed, restored.session.value != null)
            assertEquals(allowed, SessionStore(prefs).session.value != null)
        }
    }
}

@RunWith(Parameterized::class)
class AccessTokenValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            token("no-session-no-header", null, "/api/v1/profile", 200, null, false),
            token("empty-token-no-header", "", "/api/v1/profile", 200, null, true),
            token("spaces-token-no-header", "   ", "/api/v1/profile", 200, null, true),
            token("tab-token-no-header", "\t", "/api/v1/profile", 200, null, true),
            token("profile-bearer-attached", "local-token", "/api/v1/profile", 200, "Bearer local-token", true),
            token("survey-bearer-attached", "local-token", "/api/v1/surveys", 200, "Bearer local-token", true),
            token("report-bearer-attached", "local-token", "/api/v1/reports", 200, "Bearer local-token", true),
            token("ai-bearer-attached", "local-token", "/api/v1/ai/conversations", 200, "Bearer local-token", true),
            token("login-bypasses-bearer", "local-token", "/api/v1/auth/login", 200, null, true),
            token("registration-bypasses-bearer", "local-token", "/api/v1/auth/register", 200, null, true),
            token("recovery-request-bypasses-bearer", "local-token", "/api/v1/auth/password-recovery/request", 200, null, true),
            token("recovery-confirm-bypasses-bearer", "local-token", "/api/v1/auth/password-recovery/confirm", 200, null, true),
            token("auth-prefix-lookalike-protected", "local-token", "/api/v1/authx/login", 200, "Bearer local-token", true),
            token("auth-root-without-slash-protected", "local-token", "/api/v1/auth", 200, "Bearer local-token", true),
            token("protected-401-clears-session", "local-token", "/api/v1/profile", 401, "Bearer local-token", false),
            token("protected-403-retains-session", "local-token", "/api/v1/profile", 403, "Bearer local-token", true),
            token("protected-500-retains-session", "local-token", "/api/v1/profile", 500, "Bearer local-token", true),
            token("protected-503-retains-session", "local-token", "/api/v1/ai/conversations", 503, "Bearer local-token", true),
            token("public-401-does-not-clear-existing-session", "local-token", "/api/v1/auth/login", 401, null, true),
            case("cleared-session-not-sent-on-next-request") {
                runBlocking { sessions.save(session) }
                localHttp("", 401, configure = { addInterceptor(AccessTokenInterceptor(sessions)) }) { r, s ->
                    val client = r.callFactory() as OkHttpClient
                    client.newCall(Request.Builder().url(s.url("/api/v1/profile")).build()).execute().close()
                    assertEquals("Bearer local-token", s.takeRequest().getHeader("Authorization"))
                    s.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(200))
                    client.newCall(Request.Builder().url(s.url("/api/v1/profile")).build()).execute().close()
                    assertNull(s.takeRequest().getHeader("Authorization")); assertNull(sessions.currentToken()); assertTrue(prefs.values.isEmpty())
                }
            },
        )
        private fun token(name: String, value: String?, path: String, code: Int, header: String?, retained: Boolean) = case(name) {
            if (value != null) runBlocking { sessions.save(session.copy(token = value)) }
            localHttp("", code, configure = { addInterceptor(AccessTokenInterceptor(sessions)) }) { r, s ->
                val client = r.callFactory() as OkHttpClient
                client.newCall(Request.Builder().url(s.url(path)).build()).execute().use { assertEquals(code, it.code) }
                val request = s.takeRequest()
                assertEquals(header, request.getHeader("Authorization")); assertEquals(path, request.path)
                assertEquals(retained, sessions.session.value != null)
            }
        }
    }
}
