package com.experimentos.mobile.validation

import com.experimentos.mobile.survey.presentation.*
import com.experimentos.mobile.survey.data.*
import com.experimentos.mobile.comment.data.*
import com.experimentos.mobile.ai.presentation.*
import com.experimentos.mobile.ai.data.*
import com.experimentos.mobile.activity.presentation.*
import com.experimentos.mobile.activity.data.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SurveyFormValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            input("empty-input", "", ""), input("below-limit", "x".repeat(999), "x".repeat(999)),
            input("at-limit", "x".repeat(1000), "x".repeat(1000)), input("over-limit", "x".repeat(1001), "x".repeat(1000)),
            input("multiline-preserved", "a\nb", "a\nb"), input("unicode-preserved", "árbol🔒", "árbol🔒"),
            input("padding-preserved-until-submit", " answer ", " answer "), input("utf16-limit-preserved", "🔒".repeat(501), "🔒".repeat(500)),
            input("large-paste-truncated", "x".repeat(100000), "x".repeat(1000)),
            gate("empty-disabled", "", false, false), gate("spaces-disabled", "  ", false, false), gate("newlines-disabled", "\r\n\t", false, false),
            gate("unicode-space-disabled", "\u2003", false, false), gate("text-enabled", "answer", false, true), gate("busy-disabled", "answer", true, false),
            answerFlow("answer-input-to-api", " answer "), answerFlow("answer-at-boundary-to-api", "x".repeat(1001)),
            replyFlow("root-input-to-api", null), replyFlow("reply-input-to-api", 4),
            case("blank-form-blocks-answer-api") { surveys.give("getPublished", listOf(survey)); val vm = SurveyViewModel(surveys.api, comments.api); drain(); val text = surveyTextInput("\n"); if (canSubmitSurveyText(text, false)) vm.answer(1, text); drain(); assertEquals(0, surveys.count("answer")); assertTrue(vm.state.value.answeredSurveyIds.isEmpty()) },
        )
        private fun input(name: String, value: String, expected: String) = case(name, "unit") { assertEquals(expected, surveyTextInput(value)) }
        private fun gate(name: String, value: String, busy: Boolean, expected: Boolean) = case(name, "unit") { assertEquals(expected, canSubmitSurveyText(value, busy)) }
        private fun answerFlow(name: String, input: String) = case(name) { surveys.give("getPublished", listOf(survey), listOf(survey.copy(answers = 1))); surveys.give("answer", Unit); comments.give("getComments", emptyList<CommentResponse>()); val vm = SurveyViewModel(surveys.api, comments.api); drain(); val text = surveyTextInput(input); assertTrue(canSubmitSurveyText(text, false)); vm.answer(1, text); drain(); assertEquals(AnswerRequest(input.take(1000).trim()), surveys.args("answer")[1]); assertTrue(vm.state.value.answeredSurveyIds.contains(1)) }
        private fun replyFlow(name: String, parent: Long?) = case(name) { surveys.give("getPublished", listOf(survey)); comments.give("create", comment); comments.give("getComments", listOf(comment)); val vm = SurveyViewModel(surveys.api, comments.api); drain(); val text = surveyTextInput(" reply "); assertTrue(canSubmitSurveyText(text, false)); vm.createComment(1, text, parent); drain(); assertEquals(CreateCommentRequest("reply", parent), comments.args("create")[1]) }
    }
}

@RunWith(Parameterized::class)
class AiFormValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            rename("rename-empty", "", false), rename("rename-space", " ", false), rename("rename-tab-newline", "\t\n", false),
            rename("rename-unicode-space", "\u2003", false), rename("rename-normal", "Title", true), rename("rename-padded", " Title ", true),
            rename("rename-unicode", "Conversación 🔒", true), rename("rename-no-client-length-limit", "x".repeat(2000), true),
            send("message-empty", "", true, false), send("message-space", " ", true, false), send("message-newline-tab", "\n\t", true, false),
            send("message-unicode-space", "\u2003", true, false), send("message-normal", "hello", true, true), send("message-multiline", "hello\nworld", true, true),
            send("message-disabled-while-busy", "hello", false, false), send("message-no-client-length-limit", "x".repeat(10000), true, true),
            case("rename-form-to-api") { ai.give("getConversations", listOf(conversation)); ai.give("renameConversation", conversation.copy(title = "Renamed")); val vm = AiViewModel(ai.api); vm.loadConversations(); drain(); assertTrue(canRenameAiConversation(" Renamed ")); vm.renameConversation(1, " Renamed "); drain(); assertEquals(UpdateConversationRequest("Renamed"), ai.args("renameConversation")[1]); assertEquals("Renamed", vm.state.value.conversations[0].title) },
            case("send-form-to-api") { ai.give("getMessages", emptyList<MessageResponse>()); ai.give("sendMessage", listOf(message)); val vm = AiViewModel(ai.api); vm.selectConversation(1); drain(); assertTrue(canSendAiMessage(" hello ", true)); vm.sendMessage(" hello ", "es"); drain(); assertEquals(SendMessageRequest("hello", "es"), ai.args("sendMessage")[1]); assertEquals(listOf(message), vm.state.value.messages) },
            case("busy-composer-blocks-duplicate") { ai.give("getMessages", emptyList<MessageResponse>()); ai.hold("sendMessage"); val vm = AiViewModel(ai.api); vm.selectConversation(1); drain(); vm.sendMessage("hello", "es"); assertFalse(canSendAiMessage("next", !vm.state.value.isSubmitting)); drain(); assertEquals(1, ai.count("sendMessage")); ai.release("sendMessage", listOf(message)); drain(); assertTrue(canSendAiMessage("next", !vm.state.value.isSubmitting)) },
            case("blank-rename-form-no-api") { val vm = AiViewModel(ai.api); if (canRenameAiConversation(" \n")) vm.renameConversation(1, " \n"); drain(); assertTrue(ai.calls.isEmpty()) },
        )
        private fun rename(name: String, title: String, expected: Boolean) = case(name, "unit") { assertEquals(expected, canRenameAiConversation(title)) }
        private fun send(name: String, message: String, enabled: Boolean, expected: Boolean) = case(name, "unit") { assertEquals(expected, canSendAiMessage(message, enabled)) }
    }
}

@RunWith(Parameterized::class)
class ActivityFormValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            gate("no-vote-no-pending", null, null, false, false), gate("first-vote-ready", null, 10, false, true),
            gate("first-vote-busy", null, 10, true, false), gate("existing-no-change", 10, null, false, false),
            gate("same-option-no-change", 10, 10, false, false), gate("changed-option-ready", 10, 20, false, true),
            gate("changed-option-busy", 10, 20, true, false), gate("existing-no-change-busy", 10, null, true, false),
            case("valid-choice-enables-button") { val vm = formActivityVm(); vm.choose(1, 10); assertTrue(canSubmitActivityVote(vm.state.value.selectedOptions[1], vm.state.value.pendingOptions[1], false)) },
            case("unknown-choice-does-not-enable") { val vm = formActivityVm(); vm.choose(1, 99); assertFalse(canSubmitActivityVote(null, vm.state.value.pendingOptions[1], false)) },
            case("closed-choice-does-not-enable") { activities.give("getOpen", listOf(activity.copy(status = "CLOSED"))); val vm = ActivityViewModel(activities.api); drain(); vm.choose(1, 10); assertFalse(canSubmitActivityVote(null, vm.state.value.pendingOptions[1], false)) },
            case("unknown-activity-does-not-enable") { val vm = formActivityVm(); vm.choose(2, 10); assertFalse(canSubmitActivityVote(null, vm.state.value.pendingOptions[2], false)) },
            case("foreign-option-does-not-enable") { val vm = formActivityVm(); vm.choose(1, 30); assertFalse(canSubmitActivityVote(null, vm.state.value.pendingOptions[1], false)); assertEquals(0, activities.count("vote")) },
            case("vote-success-disables-no-change") { val vm = formActivityVm(); activities.give("vote", Unit); activities.give("getOpen", listOf(activity)); vm.choose(1, 10); vm.vote(1); drain(); assertFalse(canSubmitActivityVote(vm.state.value.selectedOptions[1], vm.state.value.pendingOptions[1], vm.state.value.isSubmitting)) },
            case("vote-failure-enables-retry") { val vm = formActivityVm(); activities.fail("vote"); vm.choose(1, 10); vm.vote(1); drain(); assertTrue(canSubmitActivityVote(vm.state.value.selectedOptions[1], vm.state.value.pendingOptions[1], vm.state.value.isSubmitting)) },
            case("pending-vote-disables-button") { val vm = formActivityVm(); activities.hold("vote"); activities.give("getOpen", listOf(activity)); vm.choose(1, 10); vm.vote(1); drain(); assertFalse(canSubmitActivityVote(null, 10, vm.state.value.isSubmitting)); activities.release("vote", Unit); drain() },
            case("select-same-committed-vote-disabled") { val vm = formActivityVm(); activities.give("vote", Unit); activities.give("getOpen", listOf(activity)); vm.choose(1, 10); vm.vote(1); drain(); vm.choose(1, 10); assertFalse(canSubmitActivityVote(vm.state.value.selectedOptions[1], vm.state.value.pendingOptions[1], false)) },
            case("select-new-committed-vote-enabled") { val vm = formActivityVm(); activities.give("vote", Unit); activities.give("getOpen", listOf(activity)); vm.choose(1, 10); vm.vote(1); drain(); vm.choose(1, 20); assertTrue(canSubmitActivityVote(vm.state.value.selectedOptions[1], vm.state.value.pendingOptions[1], false)) },
            case("selection-isolated-between-cards") { activities.give("getOpen", listOf(activity, activity.copy(id = 2))); val vm = ActivityViewModel(activities.api); drain(); vm.choose(1, 10); assertTrue(canSubmitActivityVote(null, vm.state.value.pendingOptions[1], false)); assertFalse(canSubmitActivityVote(null, vm.state.value.pendingOptions[2], false)) },
            case("form-vote-http-option-id-contract") { assertTrue(canSubmitActivityVote(null, 10, false)); localHttp("", 204) { r, s -> kotlinx.coroutines.runBlocking { r.create(ActivityApi::class.java).vote(1, VoteRequest(10)); assertEquals(10L, requestJson(s)["option_id"].asLong) } } },
        )
        private fun gate(name: String, selected: Long?, pending: Long?, busy: Boolean, expected: Boolean) = case(name, "unit") { assertEquals(expected, canSubmitActivityVote(selected, pending, busy)) }
    }
}
private fun TestEnv.formActivityVm(): ActivityViewModel { activities.give("getOpen", listOf(activity)); return ActivityViewModel(activities.api).also { drain() } }
