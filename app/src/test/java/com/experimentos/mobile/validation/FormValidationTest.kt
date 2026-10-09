package com.experimentos.mobile.validation

import com.experimentos.mobile.profile.presentation.*
import com.experimentos.mobile.profile.data.*
import com.experimentos.mobile.humanresources.presentation.*
import com.experimentos.mobile.activity.data.*
import com.experimentos.mobile.report.presentation.*
import com.experimentos.mobile.report.data.*
import com.experimentos.mobile.survey.data.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], application = android.app.Application::class)
// These fixtures exercise Android email patterns; they do not render graphics or use SQLite.
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Suppress("DEPRECATION") // Legacy SQLite is scoped to these validation fixtures, not database tests.
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class ProfileFormValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}") fun cases() = rows(
            field("username-below-min", ProfileField.USERNAME, "ab", false), field("username-at-min", ProfileField.USERNAME, "abc", true),
            field("username-at-max", ProfileField.USERNAME, "a".repeat(50), true), field("username-above-max", ProfileField.USERNAME, "a".repeat(51), false),
            field("username-unicode-rejected", ProfileField.USERNAME, "árbol", false), field("username-internal-space-rejected", ProfileField.USERNAME, "a na", false),
            field("username-punctuation-allowed", ProfileField.USERNAME, "a._-1", true), field("username-padded-normalized", ProfileField.USERNAME, " ana ", true),
            field("email-valid-trimmed", ProfileField.EMAIL, " ana@example.com ", true), field("email-missing-domain", ProfileField.EMAIL, "ana@", false),
            field("email-internal-space", ProfileField.EMAIL, "a na@example.com", false),
            field("name-below-min", ProfileField.DISPLAY_NAME, " A ", false), field("name-at-max", ProfileField.DISPLAY_NAME, "A".repeat(100), true),
            field("name-above-max", ProfileField.DISPLAY_NAME, "A".repeat(101), false),
            case("valid-name-disabled-during-save", "unit") { assertFalse(canSaveProfileField(ProfileField.DISPLAY_NAME, "Ana", true)) },
            case("invalid-username-disabled-while-idle", "unit") { assertFalse(canSaveProfileField(ProfileField.USERNAME, "!", false)) },
            profileFlow("name-form-to-profile-api", ProfileField.DISPLAY_NAME, " Bea ", "Bea"),
            profileFlow("username-form-to-profile-api", ProfileField.USERNAME, " bea ", "bea"),
            profileFlow("email-form-to-profile-api", ProfileField.EMAIL, " bea@example.com ", "bea@example.com"),
            case("invalid-email-form-no-api-call") { profiles.give("getProfile", profile); val vm = ProfileViewModel(profiles.api, appearance, sessions); drain(); if (canSaveProfileField(ProfileField.EMAIL, "bad", false)) vm.updateAccount("ana", "bad", "Ana"); drain(); assertEquals(0, profiles.count("updateAccount")) },
        )
        private fun field(name: String, field: ProfileField, input: String, expected: Boolean) = case(name, "unit") { assertEquals(expected, isProfileFieldValid(field, input)); assertEquals(expected, canSaveProfileField(field, input, false)) }
        private fun profileFlow(name: String, field: ProfileField, input: String, expected: String) = case(name) {
            profiles.give("getProfile", profile); profiles.give("updateAccount", UpdateAccountResponse(profile, "replacement"))
            val vm = ProfileViewModel(profiles.api, appearance, sessions); drain()
            assertTrue(canSaveProfileField(field, input, false))
            vm.updateAccount(if (field == ProfileField.USERNAME) input else "ana", if (field == ProfileField.EMAIL) input else "ana@example.com", if (field == ProfileField.DISPLAY_NAME) input else "Ana")
            drain(); val request = profiles.args("updateAccount")[0] as UpdateAccountRequest
            assertEquals(expected, when (field) { ProfileField.USERNAME -> request.username; ProfileField.EMAIL -> request.email; ProfileField.DISPLAY_NAME -> request.displayName })
            assertFalse(vm.state.value.isSaving)
        }
    }
}

@RunWith(Parameterized::class)
class HrFormValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            limit("title-below-limit", 119, 119, ::hrTitleInput), limit("title-at-limit", 120, 120, ::hrTitleInput), limit("title-over-limit", 121, 120, ::hrTitleInput),
            limit("detail-below-limit", 499, 499, ::hrDetailInput), limit("detail-at-limit", 500, 500, ::hrDetailInput), limit("detail-over-limit", 501, 500, ::hrDetailInput),
            limit("options-below-limit", 999, 999, ::hrOptionsInput), limit("options-at-limit", 1000, 1000, ::hrOptionsInput), limit("options-over-limit", 1001, 1000, ::hrOptionsInput),
            case("survey-blank-title", "unit") { assertFalse(canCreateSurvey(" ", "Question", false)) },
            case("survey-blank-question", "unit") { assertFalse(canCreateSurvey("Title", "\t", false)) },
            case("survey-busy", "unit") { assertFalse(canCreateSurvey("Title", "Question", true)) },
            case("activity-one-option", "unit") { assertFalse(canCreateActivity("Title", "A\n ", false)) },
            case("activity-blank-title", "unit") { assertFalse(canCreateActivity("\n", "A\nB", false)) },
            case("activity-busy", "unit") { assertFalse(canCreateActivity("Title", "A\nB", true)) },
            case("activity-windows-newlines", "unit") { assertTrue(canCreateActivity("Title", "A\r\n \r\nB", false)) },
            case("daily-survey-form-to-api") { hrFlow(SurveyType.DAILY, true) },
            case("weekly-survey-form-no-comments-to-api") { hrFlow(SurveyType.WEEKLY, false) },
            case("activity-form-normalized-to-api") { prepareHr(); activities.give("create", activity); val vm = HrContentViewModel(surveys.api, activities.api, comments.api); drain(); val t = hrTitleInput(" Title "); val o = hrOptionsInput(" A \n\n B "); assertTrue(canCreateActivity(t, o, false)); vm.createActivity(t, hrDetailInput(" Details "), o); drain(); assertEquals(CreateActivityRequest("Title", "Details", listOf("A", "B")), activities.args("create")[0]) },
            case("truncated-options-cannot-create-one-option") { prepareHr(); val vm = HrContentViewModel(surveys.api, activities.api, comments.api); drain(); val o = hrOptionsInput("A".repeat(1000) + "\nB"); assertFalse(canCreateActivity("Title", o, false)); vm.createActivity("Title", "", o); drain(); assertEquals(0, activities.count("create")); assertNotNull(vm.state.value.errorMessage) },
            case("percentage-zero-total", "unit") { assertEquals(0, calculatePercentage(10, 0)) },
            case("percentage-negative-total", "unit") { assertEquals(0, calculatePercentage(10, -1)) },
            case("percentage-negative-part", "unit") { assertEquals(0, calculatePercentage(-1, 10)) },
            case("percentage-rounding", "unit") { assertEquals(33, calculatePercentage(1, 3)) },
            case("percentage-half", "unit") { assertEquals(50, calculatePercentage(1, 2)) },
            case("percentage-clamped-above-total", "unit") { assertEquals(100, calculatePercentage(20, 10)) },
            case("percentage-large-count-no-integer-overflow", "unit") { assertEquals(100, calculatePercentage(Long.MAX_VALUE, Long.MAX_VALUE)) },
        )
        private fun limit(name: String, input: Int, expected: Int, normalize: (String) -> String) = case(name, "unit") { val value = "x".repeat(input); assertEquals(value.take(expected), normalize(value)) }
    }
}
private fun TestEnv.prepareHr() { surveys.give("getManaged", emptyList<SurveyResponse>()); activities.give("getManaged", emptyList<ActivityResponse>()) }
private fun TestEnv.hrFlow(type: SurveyType, commentsAllowed: Boolean) {
    prepareHr(); surveys.give("create", survey.copy(type = type, allowComments = commentsAllowed))
    val vm = HrContentViewModel(surveys.api, activities.api, comments.api); drain()
    val title = hrTitleInput(" Title "); val question = hrDetailInput(" Question ")
    assertTrue(canCreateSurvey(title, question, false)); vm.createSurvey(title, question, type, commentsAllowed); drain()
    assertEquals(CreateSurveyRequest("Title", "Question", type, commentsAllowed), surveys.args("create")[0])
}

@RunWith(Parameterized::class)
class ReportFormValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            missing("category-empty", "", "T", "D", "LOW", "puesto"), missing("category-blank", " \t", "T", "D", "LOW", "puesto"),
            missing("title-empty", "Ops", "", "D", "LOW", "título"), missing("title-blank", "Ops", "\n", "D", "LOW", "título"),
            missing("description-empty", "Ops", "T", "", "LOW", "Describe"), missing("description-blank", "Ops", "T", " ", "LOW", "Describe"),
            missing("priority-empty", "Ops", "T", "D", "", "prioridad"), missing("priority-blank", "Ops", "T", "D", "\t", "prioridad"),
            missing("first-error-order", "", "", "", "", "puesto"),
            case("busy-valid-form-blocked", "unit") { assertFalse(canSubmitReport("Ops", "T", "D", "LOW", true)) },
            case("minimal-valid-form", "unit") { assertTrue(canSubmitReport("O", "T", "D", "LOW", false)) },
            case("padded-fields-accepted", "unit") { assertNull(reportValidationError(" O ", " T ", " D ", " LOW ")) },
            case("title-below-limit", "unit") { assertEquals(159, reportTitleInput("a".repeat(159)).length) },
            case("title-at-limit", "unit") { assertEquals(160, reportTitleInput("a".repeat(160)).length) },
            case("title-over-limit", "unit") { assertEquals(160, reportTitleInput("a".repeat(161)).length) },
            case("description-at-limit", "unit") { assertEquals(2000, reportDescriptionInput("a".repeat(2000)).length) },
            case("description-over-limit", "unit") { assertEquals(2000, reportDescriptionInput("a".repeat(2001)).length) },
            reportFlow("anonymous-report-flow", true), reportFlow("identified-report-flow", false),
            case("invalid-form-blocks-submit") { val vm = ReportViewModel(reports.api); if (canSubmitReport("", "T", "D", "LOW", false)) vm.create(reportRequest); drain(); assertTrue(reports.calls.isEmpty()); assertNull(vm.state.value.message) },
        )
        private fun missing(name: String, c: String, t: String, d: String, p: String, error: String) = case(name, "unit") { assertTrue(reportValidationError(c, t, d, p)!!.contains(error)); assertFalse(canSubmitReport(c, t, d, p, false)) }
        private fun reportFlow(name: String, anonymous: Boolean) = case(name) {
            reports.give("create", report.copy(anonymous = anonymous)); val vm = ReportViewModel(reports.api); var back = false
            val t = reportTitleInput("Title" + "x".repeat(200)); val d = reportDescriptionInput("Description" + "x".repeat(2100))
            assertTrue(canSubmitReport("Ops", t, d, "LOW", false)); vm.create(CreateReportRequest("Ops", t, d, "LOW", anonymous)) { back = true }; drain()
            val sent = reports.args("create")[0] as CreateReportRequest
            assertEquals(160, sent.title.length); assertEquals(2000, sent.description.length); assertEquals(anonymous, sent.anonymous); assertTrue(back)
        }
    }
}
