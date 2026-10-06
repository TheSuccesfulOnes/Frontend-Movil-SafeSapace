package com.experimentos.mobile.validation

import com.experimentos.mobile.survey.presentation.*
import com.experimentos.mobile.survey.data.*
import com.experimentos.mobile.comment.data.*
import com.experimentos.mobile.humanresources.presentation.*
import com.experimentos.mobile.activity.data.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private fun TestEnv.surveyVm(): SurveyViewModel { surveys.give("getPublished", listOf(survey)); return SurveyViewModel(surveys.api, comments.api).also { drain() } }

@RunWith(Parameterized::class)
class SurveyViewModelValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("answer-empty-blocked", "unit") { val vm = surveyVm(); vm.answer(1, ""); assertEquals(0, surveys.count("answer")); assertNotNull(vm.state.value.errorMessage) },
            case("answer-whitespace-blocked", "unit") { val vm = surveyVm(); vm.answer(1, "\n \t"); assertEquals(0, surveys.count("answer")); assertFalse(vm.state.value.isSubmitting) },
            case("root-comment-empty-blocked", "unit") { val vm = surveyVm(); vm.createComment(1, ""); assertEquals(0, comments.count("create")); assertNotNull(vm.state.value.errorMessage) },
            case("reply-blank-blocked", "unit") { val vm = surveyVm(); vm.createComment(1, " \n", 4); assertEquals(0, comments.count("create")); assertNotNull(vm.state.value.errorMessage) },
            case("load-success") { val vm = surveyVm(); assertEquals(listOf(survey), vm.state.value.surveys); assertFalse(vm.state.value.isLoading) },
            case("load-failure-and-retry") { surveys.fail("getPublished"); val vm = SurveyViewModel(surveys.api, comments.api); drain(); assertNotNull(vm.state.value.errorMessage); surveys.give("getPublished", listOf(survey)); vm.load(); drain(); assertNull(vm.state.value.errorMessage); assertEquals(1, vm.state.value.surveys.size) },
            case("answer-trim-refresh-count-and-comments") { val vm = surveyVm(); surveys.give("answer", Unit); surveys.give("getPublished", listOf(survey.copy(answers = 1))); comments.give("getComments", listOf(comment)); vm.answer(1, " answer "); drain(); assertEquals(AnswerRequest("answer"), surveys.args("answer")[1]); assertTrue(vm.state.value.answeredSurveyIds.contains(1)); assertEquals(1L, vm.state.value.surveys[0].answers); assertEquals(listOf(comment), vm.state.value.comments[1]); assertFalse(vm.state.value.isSubmitting) },
            case("answer-conflict-not-marked-answered") { val vm = surveyVm(); surveys.fail("answer", httpError(409, "already been answered")); vm.answer(1, "answer"); drain(); assertTrue(vm.state.value.answeredSurveyIds.isEmpty()); assertTrue(vm.state.value.errorMessage!!.contains("respondiste")) },
            case("answer-closed-survey-error") { val vm = surveyVm(); surveys.fail("answer", httpError(400, "survey is not open")); vm.answer(1, "answer"); drain(); assertTrue(vm.state.value.errorMessage!!.contains("disponible")); assertFalse(vm.state.value.isSubmitting) },
            case("answer-refresh-failures-preserve-success") { val vm = surveyVm(); surveys.give("answer", Unit); surveys.fail("getPublished"); comments.fail("getComments"); vm.answer(1, "answer"); drain(); assertTrue(vm.state.value.answeredSurveyIds.contains(1)); assertNotNull(vm.state.value.message); assertNull(vm.state.value.errorMessage) },
            case("comments-collapse-reopen-uses-cache") { val vm = surveyVm(); comments.give("getComments", listOf(comment)); vm.toggleComments(1); drain(); vm.toggleComments(1); assertNull(vm.state.value.expandedSurveyId); vm.toggleComments(1); drain(); assertEquals(1, comments.count("getComments")) },
            case("comments-separate-survey-caches") { val vm = surveyVm(); comments.give("getComments", listOf(comment), emptyList<CommentResponse>()); vm.toggleComments(1); drain(); vm.toggleComments(2); drain(); assertEquals(2, vm.state.value.comments.size); assertEquals(2L, vm.state.value.expandedSurveyId) },
            case("comments-load-failure-retry") { val vm = surveyVm(); comments.fail("getComments"); vm.toggleComments(1); drain(); assertNotNull(vm.state.value.errorMessage); vm.toggleComments(1); comments.give("getComments", listOf(comment)); vm.toggleComments(1); drain(); assertEquals(listOf(comment), vm.state.value.comments[1]); assertNull(vm.state.value.errorMessage) },
            case("root-comment-trim-and-refresh") { val vm = surveyVm(); comments.give("create", comment); comments.give("getComments", listOf(comment)); vm.createComment(1, " root "); drain(); assertEquals(CreateCommentRequest("root", null), comments.args("create")[1]); assertNotNull(vm.state.value.message) },
            case("reply-retains-parent-id") { val vm = surveyVm(); comments.give("create", comment); comments.give("getComments", listOf(comment.copy(replies = listOf(comment.copy(id = 5))))); vm.createComment(1, " reply ", 4); drain(); assertEquals(CreateCommentRequest("reply", 4), comments.args("create")[1]); assertEquals(5L, vm.state.value.comments[1]!![0].replies[0].id) },
            case("reply-forbidden-not-added") { val vm = surveyVm(); comments.fail("create", httpError(403)); vm.createComment(1, "reply", 4); drain(); assertTrue(vm.state.value.comments.isEmpty()); assertTrue(vm.state.value.errorMessage!!.contains("permisos")); assertFalse(vm.state.value.isSubmitting) },
            case("like-refreshes-likes") { val vm = surveyVm(); comments.give("like", Unit); comments.give("getComments", listOf(comment.copy(likes = 1))); vm.likeComment(1, 4); drain(); assertEquals(listOf(1L, 4L), comments.args("like")); assertEquals(1L, vm.state.value.comments[1]!![0].likes) },
            case("delete-refreshes-thread") { val vm = surveyVm(); comments.give("delete", Unit); comments.give("getComments", emptyList<CommentResponse>()); vm.deleteComment(1, 4); drain(); assertEquals(listOf(1L, 4L), comments.args("delete")); assertTrue(vm.state.value.comments[1]!!.isEmpty()); assertNotNull(vm.state.value.message) },
            case("delete-forbidden-clear-feedback") { val vm = surveyVm(); comments.fail("delete", httpError(403)); vm.deleteComment(1, 4); drain(); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isSubmitting); vm.clearMessage(); assertNull(vm.state.value.errorMessage); assertNull(vm.state.value.message) },
            case("like-failure-does-not-refresh") { val vm = surveyVm(); comments.fail("like"); vm.likeComment(1, 4); drain(); assertNotNull(vm.state.value.errorMessage); assertEquals(0, comments.count("getComments")); assertFalse(vm.state.value.isSubmitting) },
        )
    }
}

private fun TestEnv.hrVm(): HrContentViewModel {
    surveys.give("getManaged", listOf(survey)); activities.give("getManaged", listOf(activity))
    return HrContentViewModel(surveys.api, activities.api, comments.api).also { drain() }
}

@RunWith(Parameterized::class)
class HrViewModelsValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("survey-blank-title-blocked", "unit") { val vm = hrVm(); vm.createSurvey(" ", "Q", SurveyType.DAILY, true); assertEquals(0, surveys.count("create")); assertNotNull(vm.state.value.errorMessage) },
            case("survey-blank-question-blocked", "unit") { val vm = hrVm(); vm.createSurvey("T", "", SurveyType.WEEKLY, false); assertEquals(0, surveys.count("create")) },
            case("activity-blank-title-blocked", "unit") { val vm = hrVm(); vm.createActivity(" ", "", "A\nB"); assertEquals(0, activities.count("create")) },
            case("activity-single-option-blocked", "unit") { val vm = hrVm(); vm.createActivity("T", "D", "A\n \n"); assertEquals(0, activities.count("create")); assertNotNull(vm.state.value.errorMessage) },
            case("survey-create-normalizes-and-prepends") { val vm = hrVm(); surveys.give("create", survey.copy(id = 2, type = SurveyType.WEEKLY, allowComments = false)); vm.createSurvey(" T ", " Q ", SurveyType.WEEKLY, false); drain(); assertEquals(CreateSurveyRequest("T", "Q", SurveyType.WEEKLY, false), surveys.args("create")[0]); assertEquals(listOf(2L, 1L), vm.state.value.surveys.map { it.id }) },
            case("survey-create-forbidden") { val vm = hrVm(); surveys.fail("create", httpError(403)); vm.createSurvey("T", "Q", SurveyType.DAILY, true); drain(); assertEquals(1, vm.state.value.surveys.size); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isSubmitting) },
            case("activity-two-options-normalized") { val vm = hrVm(); activities.give("create", activity.copy(id = 2)); vm.createActivity(" T ", " D ", " A \r\n\r\n B "); drain(); assertEquals(CreateActivityRequest("T", "D", listOf("A", "B")), activities.args("create")[0]); assertEquals(listOf(2L, 1L), vm.state.value.activities.map { it.id }) },
            case("activity-optional-description-empty") { val vm = hrVm(); activities.give("create", activity); vm.createActivity("T", " ", "A\nB\nC"); drain(); assertEquals("", (activities.args("create")[0] as CreateActivityRequest).description); assertEquals(3, (activities.args("create")[0] as CreateActivityRequest).options.size) },
            case("activity-create-failure-preserves-list") { val vm = hrVm(); activities.fail("create"); vm.createActivity("T", "D", "A\nB"); drain(); assertEquals(listOf(activity), vm.state.value.activities); assertFalse(vm.state.value.isSubmitting) },
            case("publish-replaces-survey") { val vm = hrVm(); surveys.give("publish", survey.copy(status = "PUBLISHED", answers = 4)); vm.changeSurveyStatus(1, true); drain(); assertEquals(4L, vm.state.value.surveys[0].answers); assertEquals(1L, surveys.args("publish")[0]) },
            case("close-removes-survey") { val vm = hrVm(); surveys.give("close", survey.copy(status = "CLOSED")); vm.changeSurveyStatus(1, false); drain(); assertTrue(vm.state.value.surveys.isEmpty()); assertNotNull(vm.state.value.message) },
            case("status-failure-preserves-survey") { val vm = hrVm(); surveys.fail("close", httpError(403)); vm.changeSurveyStatus(1, false); drain(); assertEquals(listOf(survey), vm.state.value.surveys); assertNotNull(vm.state.value.errorMessage) },
            case("close-activity-removes-item") { val vm = hrVm(); activities.give("close", Unit); vm.closeActivity(1); drain(); assertTrue(vm.state.value.activities.isEmpty()); assertFalse(vm.state.value.isSubmitting) },
            case("close-activity-failure-preserves-item") { val vm = hrVm(); activities.fail("close"); vm.closeActivity(1); drain(); assertEquals(listOf(activity), vm.state.value.activities); vm.clearFeedback(); assertNull(vm.state.value.errorMessage) },
            case("load-partial-failure-not-published") { surveys.give("getManaged", listOf(survey)); activities.fail("getManaged"); val vm = HrContentViewModel(surveys.api, activities.api, comments.api); drain(); assertTrue(vm.state.value.surveys.isEmpty()); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isLoading) },
            case("reload-clears-comments-and-selection") { val vm = hrVm(); comments.give("getComments", listOf(comment)); vm.toggleSurveyComments(1); drain(); surveys.give("getManaged", listOf(survey)); activities.give("getManaged", listOf(activity)); vm.load(); drain(); assertTrue(vm.state.value.comments.isEmpty()); assertNull(vm.state.value.expandedSurveyId) },
            case("comments-collapse-reopen-cached") { val vm = hrVm(); comments.give("getComments", listOf(comment)); vm.toggleSurveyComments(1); drain(); vm.toggleSurveyComments(1); assertNull(vm.state.value.expandedSurveyId); vm.toggleSurveyComments(1); drain(); assertEquals(1, comments.count("getComments")); assertNull(vm.state.value.commentsLoadingId) },
            case("comments-failure-isolated") { val vm = hrVm(); comments.fail("getComments", httpError(403)); vm.toggleSurveyComments(1); drain(); assertNotNull(vm.state.value.commentsErrorMessage); assertNull(vm.state.value.errorMessage); assertNull(vm.state.value.commentsLoadingId) },
            case("wellbeing-summary-success") { val summary = com.experimentos.mobile.mood.data.MoodSummary("2026-10-05", 0, emptyMap()); moods.give("getSummary", summary); val vm = HrHomeViewModel(moods.api); drain(); assertEquals(summary, vm.state.value.summary); assertFalse(vm.state.value.isLoading) },
            case("wellbeing-summary-failure-retry") { moods.fail("getSummary"); val vm = HrHomeViewModel(moods.api); drain(); assertNotNull(vm.state.value.errorMessage); moods.give("getSummary", com.experimentos.mobile.mood.data.MoodSummary("2026-10-05", 0, emptyMap())); vm.load(); drain(); assertNull(vm.state.value.errorMessage) },
        )
    }
}
