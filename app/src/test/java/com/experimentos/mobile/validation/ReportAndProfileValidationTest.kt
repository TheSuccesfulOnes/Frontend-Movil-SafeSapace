package com.experimentos.mobile.validation

import com.experimentos.mobile.report.presentation.*
import com.experimentos.mobile.report.data.*
import com.experimentos.mobile.profile.presentation.*
import com.experimentos.mobile.profile.data.*
import com.experimentos.mobile.shared.data.SessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReportViewModelValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("employee-initial-no-fetch", "unit") { val vm = ReportViewModel(reports.api); drain(); assertTrue(reports.calls.isEmpty()); assertFalse(vm.state.value.isLoading) },
            case("hr-initial-fetches-all") { reports.give("getAll", listOf(report)); val vm = ReportViewModel(reports.api, true); drain(); assertEquals(listOf(report), vm.state.value.reports); assertEquals(0, reports.count("getMine")) },
            case("employee-fetches-only-mine") { reports.give("getMine", listOf(report)); val vm = ReportViewModel(reports.api); vm.loadReports(); drain(); assertEquals(listOf(report), vm.state.value.reports); assertEquals(0, reports.count("getAll")) },
            case("empty-list-valid") { reports.give("getMine", emptyList<ReportResponse>()); val vm = ReportViewModel(reports.api); vm.loadReports(); drain(); assertNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isLoading) },
            case("load-failure") { reports.fail("getMine"); val vm = ReportViewModel(reports.api); vm.loadReports(); drain(); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isLoading) },
            case("load-retry-clears-error") { reports.fail("getMine"); reports.give("getMine", listOf(report)); val vm = ReportViewModel(reports.api); vm.loadReports(); drain(); vm.loadReports(); drain(); assertNull(vm.state.value.errorMessage) },
            create("anonymous-create", true), create("identified-create", false),
            case("create-callback-only-on-success") { reports.give("create", report); val vm = ReportViewModel(reports.api); var calls = 0; vm.create(reportRequest) { calls++ }; drain(); assertEquals(1, calls); assertNotNull(vm.state.value.message) },
            case("create-validation-failure-no-callback") { reports.fail("create", httpError(400)); val vm = ReportViewModel(reports.api); var called = false; vm.create(reportRequest) { called = true }; drain(); assertFalse(called); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isSubmitting) },
            case("create-network-failure") { reports.fail("create"); val vm = ReportViewModel(reports.api); vm.create(reportRequest); drain(); assertNull(vm.state.value.message); assertNotNull(vm.state.value.errorMessage) },
            case("create-loading-lifecycle") { reports.hold("create"); val vm = ReportViewModel(reports.api); vm.create(reportRequest); drain(); assertTrue(vm.state.value.isSubmitting); reports.release("create", report); drain(); assertFalse(vm.state.value.isSubmitting) },
            status("status-in-review", "IN_REVIEW"), status("status-resolved", "RESOLVED"), status("status-dismissed", "DISMISSED"),
            case("status-unknown-id-does-not-insert") { reports.give("getAll", listOf(report)); reports.give("updateStatus", report.copy(id = 99, status = "RESOLVED")); val vm = ReportViewModel(reports.api, true); drain(); vm.updateStatus(99, "RESOLVED"); drain(); assertEquals(listOf(report), vm.state.value.reports) },
            case("status-forbidden-preserves-list") { reports.give("getAll", listOf(report)); reports.fail("updateStatus", httpError(403)); val vm = ReportViewModel(reports.api, true); drain(); vm.updateStatus(1, "RESOLVED"); drain(); assertEquals(listOf(report), vm.state.value.reports); assertNotNull(vm.state.value.errorMessage) },
            case("status-retry-clears-error") { reports.give("getAll", listOf(report)); reports.fail("updateStatus"); reports.give("updateStatus", report.copy(status = "RESOLVED")); val vm = ReportViewModel(reports.api, true); drain(); vm.updateStatus(1, "RESOLVED"); drain(); vm.updateStatus(1, "RESOLVED"); drain(); assertNull(vm.state.value.errorMessage); assertEquals("RESOLVED", vm.state.value.reports.single().status) },
            case("clear-feedback-retains-data") { reports.give("create", report); val vm = ReportViewModel(reports.api); vm.create(reportRequest); drain(); vm.clearMessage(); assertNull(vm.state.value.message); assertNull(vm.state.value.errorMessage) },
            case("report-http-contract") { localHttp(REPORT_JSON) { r, s -> runBlocking { assertEquals(report, r.create(ReportApi::class.java).create(reportRequest)); val json = requestJson(s); assertTrue(json["anonymous"].asBoolean); assertEquals("LOW", json["priority"].asString); assertEquals("Ops", json["category"].asString) } } },
        )
        private fun create(name: String, anonymous: Boolean) = case(name) { reports.give("create", report.copy(anonymous = anonymous)); val vm = ReportViewModel(reports.api); vm.create(reportRequest.copy(anonymous = anonymous)); drain(); assertEquals(reportRequest.copy(anonymous = anonymous), reports.args("create")[0]); assertFalse(vm.state.value.isSubmitting) }
        private fun status(name: String, status: String) = case(name) { reports.give("getAll", listOf(report, report.copy(id = 2))); reports.give("updateStatus", report.copy(status = status)); val vm = ReportViewModel(reports.api, true); drain(); vm.updateStatus(1, status); drain(); assertEquals(UpdateReportStatusRequest(status), reports.args("updateStatus")[1]); assertEquals(status, vm.state.value.reports[0].status); assertEquals("NEW", vm.state.value.reports[1].status); assertFalse(vm.state.value.isSubmitting) }
    }
}

@RunWith(Parameterized::class)
class ProfileViewModelValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("initial-load-syncs-local-preferences") { profiles.give("getProfile", profile.copy(language = "en", theme = "DARK")); val vm = ProfileViewModel(profiles.api, appearance, sessions); drain(); assertEquals("DARK", appearance.theme.value); assertEquals("en", appearance.language.value); assertEquals(7L, vm.state.value.profile!!.userId) },
            case("profile-load-failure") { profiles.fail("getProfile"); val vm = ProfileViewModel(profiles.api, appearance, sessions); drain(); assertNull(vm.state.value.profile); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isLoading) },
            case("profile-load-retry") { profiles.fail("getProfile"); profiles.give("getProfile", profile); val vm = ProfileViewModel(profiles.api, appearance, sessions); drain(); vm.load(); drain(); assertEquals(profile, vm.state.value.profile); assertNull(vm.state.value.errorMessage) },
            case("profile-load-pending") { profiles.hold("getProfile"); val vm = ProfileViewModel(profiles.api, appearance, sessions); drain(); assertTrue(vm.state.value.isLoading); profiles.release("getProfile", profile); drain(); assertFalse(vm.state.value.isLoading) },
            preference("preferences-english-dark", "en", "DARK"), preference("preferences-spanish-light", "es", "LIGHT"),
            case("preferences-authoritative-response-normalized") { val vm = profileVm(); profiles.give("updatePreferences", profile.copy(language = "EN", theme = "dark")); vm.updatePreferences("en", "DARK"); drain(); assertEquals("en", appearance.language.value); assertEquals("DARK", appearance.theme.value) },
            case("preferences-failure-keeps-profile") { val vm = profileVm(); profiles.fail("updatePreferences", httpError(403)); vm.updatePreferences("en", "DARK"); drain(); assertEquals(profile, vm.state.value.profile); assertEquals("LIGHT", appearance.theme.value); assertFalse(vm.state.value.isSaving) },
            case("preferences-pending-state") { val vm = profileVm(); profiles.hold("updatePreferences"); vm.updatePreferences("en", "DARK"); drain(); assertTrue(vm.state.value.isSaving); profiles.release("updatePreferences", profile); drain(); assertFalse(vm.state.value.isSaving) },
            case("account-trims-fields") { val vm = profileVm(); profiles.give("updateAccount", UpdateAccountResponse(profile, "new-token")); vm.updateAccount(" ana ", " ana@example.test ", " Ana "); drain(); assertEquals(UpdateAccountRequest("ana", "ana@example.test", "Ana"), profiles.args("updateAccount")[0]) },
            case("account-empty-email-is-null") { val vm = profileVm(); profiles.give("updateAccount", UpdateAccountResponse(profile, "new-token")); vm.updateAccount("ana", "", "Ana"); drain(); assertNull((profiles.args("updateAccount")[0] as UpdateAccountRequest).email) },
            case("account-blank-email-is-null") { val vm = profileVm(); profiles.give("updateAccount", UpdateAccountResponse(profile, "new-token")); vm.updateAccount("ana", " \t", "Ana"); drain(); assertNull((profiles.args("updateAccount")[0] as UpdateAccountRequest).email) },
            case("account-updates-session-token-claims") { runBlocking { sessions.save(session) }; val vm = profileVm(); profiles.give("updateAccount", UpdateAccountResponse(profile.copy(username = "bea", displayName = "Bea"), "new-token")); vm.updateAccount("bea", "a@example.test", "Bea"); drain(); assertEquals("new-token", sessions.currentToken()); assertEquals("bea", SessionStore(prefs).session.value!!.username); assertEquals("user:7", sessions.session.value!!.accountKey) },
            case("account-update-without-session-does-not-login") { val vm = profileVm(); profiles.give("updateAccount", UpdateAccountResponse(profile, "new-token")); vm.updateAccount("ana", "", "Ana"); drain(); assertNull(sessions.currentToken()); assertNotNull(vm.state.value.message) },
            case("account-email-conflict-safe-message") { val vm = profileVm(); profiles.fail("updateAccount", httpError(409, "email already exists")); vm.updateAccount("ana", "a@example.test", "Ana"); drain(); assertTrue(vm.state.value.errorMessage!!.contains("correo")); assertEquals(profile, vm.state.value.profile) },
            case("account-failure-keeps-session") { runBlocking { sessions.save(session) }; val vm = profileVm(); profiles.fail("updateAccount"); vm.updateAccount("bea", "", "Bea"); drain(); assertEquals(session, sessions.session.value); assertFalse(vm.state.value.isSaving) },
            case("account-pending-state") { val vm = profileVm(); profiles.hold("updateAccount"); vm.updateAccount("ana", "", "Ana"); drain(); assertTrue(vm.state.value.isSaving); profiles.release("updateAccount", UpdateAccountResponse(profile, "new-token")); drain(); assertFalse(vm.state.value.isSaving) },
            case("feedback-clear-retains-profile") { val vm = profileVm(); profiles.fail("updateAccount"); vm.updateAccount("ana", "", "Ana"); drain(); vm.clearFeedback(); assertNull(vm.state.value.errorMessage); assertNull(vm.state.value.message); assertEquals(profile, vm.state.value.profile) },
            case("account-server-preferences-scoped-to-id") { val vm = profileVm(); profiles.give("updateAccount", UpdateAccountResponse(profile.copy(language = "en", theme = "DARK"), "new-token")); vm.updateAccount("ana", "", "Ana"); drain(); appearance.activateAccount("user:8"); assertEquals("LIGHT", appearance.theme.value); appearance.activateAccount("user:7"); assertEquals("DARK", appearance.theme.value) },
            case("profile-http-snake-case-contract") { localHttp("{\"profile\":$PROFILE_JSON,\"token\":\"new-token\"}") { r, s -> runBlocking { val response = r.create(ProfileApi::class.java).updateAccount(UpdateAccountRequest("ana", null, "Ana")); assertEquals(profile, response.profile); val json = requestJson(s); assertEquals("Ana", json["display_name"].asString); assertFalse(json.has("email")); assertFalse(json.has("displayName")) } } },
        )
        private fun preference(name: String, language: String, theme: String) = case(name) { val vm = profileVm(); profiles.give("updatePreferences", profile.copy(language = language, theme = theme)); vm.updatePreferences(language, theme); drain(); assertEquals(UpdatePreferencesRequest(language, theme), profiles.args("updatePreferences")[0]); assertEquals(theme, appearance.theme.value); assertEquals(language, appearance.language.value); assertNotNull(vm.state.value.message) }
    }
}
private fun TestEnv.profileVm(): ProfileViewModel { profiles.give("getProfile", profile); return ProfileViewModel(profiles.api, appearance, sessions).also { drain() } }
