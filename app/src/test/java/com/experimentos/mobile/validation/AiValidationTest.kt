package com.experimentos.mobile.validation

import com.experimentos.mobile.ai.presentation.*
import com.experimentos.mobile.ai.data.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private fun TestEnv.aiVm(): AiViewModel { ai.give("getConversations", listOf(conversation, conversation.copy(id = 2))); return AiViewModel(ai.api).also { it.loadConversations(); drain() } }
private fun TestEnv.select(vm: AiViewModel) { ai.give("getMessages", emptyList<MessageResponse>()); vm.selectConversation(1); drain() }

@RunWith(Parameterized::class)
class AiViewModelValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("send-without-conversation-blocked", "unit") { val vm = AiViewModel(ai.api); vm.sendMessage("hello", "es"); assertNotNull(vm.state.value.errorMessage); assertTrue(ai.calls.isEmpty()) },
            case("rename-blank-blocked", "unit") { val vm = AiViewModel(ai.api); vm.renameConversation(1, " \n"); drain(); assertTrue(ai.calls.isEmpty()) },
            case("send-blank-blocked", "unit") { val vm = aiVm(); select(vm); vm.sendMessage(" \t", "es"); assertEquals(0, ai.count("sendMessage")); assertTrue(vm.state.value.messages.isEmpty()) },
            case("list-conversations-success") { val vm = aiVm(); assertEquals(2, vm.state.value.conversations.size); assertFalse(vm.state.value.isLoading); assertNull(vm.state.value.selectedConversationId) },
            case("list-conversations-failure-retry") { ai.fail("getConversations"); val vm = AiViewModel(ai.api); vm.loadConversations(); drain(); assertNotNull(vm.state.value.errorMessage); ai.give("getConversations", listOf(conversation)); vm.loadConversations(); drain(); assertNull(vm.state.value.errorMessage) },
            case("create-selects-new-and-clears-messages") { val vm = aiVm(); select(vm); ai.give("createConversation", conversation.copy(id = 3)); vm.createConversation(); drain(); assertEquals(3L, vm.state.value.selectedConversationId); assertEquals(3L, vm.state.value.conversations[0].id); assertTrue(vm.state.value.messages.isEmpty()) },
            case("create-failure-preserves-list") { val vm = aiVm(); ai.fail("createConversation"); vm.createConversation(); drain(); assertEquals(2, vm.state.value.conversations.size); assertFalse(vm.state.value.isSubmitting); assertNotNull(vm.state.value.errorMessage) },
            case("select-loads-history") { val vm = aiVm(); ai.give("getMessages", listOf(message)); vm.selectConversation(1); drain(); assertEquals(listOf(message), vm.state.value.messages); assertEquals(1L, ai.args("getMessages")[0]) },
            case("stale-history-response-ignored") { val vm = aiVm(); ai.hold("getMessages"); vm.selectConversation(1); drain(); ai.give("getMessages", listOf(message.copy(id = 3))); vm.selectConversation(2); drain(); ai.release("getMessages", listOf(message)); drain(); assertEquals(2L, vm.state.value.selectedConversationId); assertEquals(3L, vm.state.value.messages[0].id) },
            case("stale-history-error-ignored") { val vm = aiVm(); ai.hold("getMessages"); vm.selectConversation(1); drain(); vm.clearSelection(); ai.reject("getMessages", java.io.IOException()); drain(); assertNull(vm.state.value.errorMessage); assertNull(vm.state.value.selectedConversationId) },
            case("send-trim-language-and-optimistic-replacement") { val vm = aiVm(); select(vm); ai.hold("sendMessage"); vm.sendMessage(" hello ", "en"); assertTrue(vm.state.value.isSending); assertEquals("hello", vm.state.value.messages.single().content); assertEquals("USER", vm.state.value.messages.single().sender); drain(); assertEquals(SendMessageRequest("hello", "en"), ai.args("sendMessage")[1]); ai.release("sendMessage", listOf(message)); drain(); assertEquals(listOf(message), vm.state.value.messages); assertFalse(vm.state.value.isSending) },
            case("send-busy-guard") { val vm = aiVm(); select(vm); ai.hold("sendMessage"); vm.sendMessage("one", "es"); vm.sendMessage("two", "es"); drain(); assertEquals(1, ai.count("sendMessage")); assertEquals(1, vm.state.value.messages.size); ai.release("sendMessage", listOf(message)); drain() },
            case("selection-and-clear-guard-during-send") { val vm = aiVm(); select(vm); ai.hold("sendMessage"); vm.sendMessage("hello", "es"); vm.selectConversation(2); vm.clearSelection(); assertEquals(1L, vm.state.value.selectedConversationId); drain(); ai.release("sendMessage", listOf(message)); drain() },
            case("send-failure-removes-optimistic-message") { val vm = aiVm(); select(vm); ai.fail("sendMessage", httpError(503)); vm.sendMessage("hello", "es"); drain(); assertTrue(vm.state.value.messages.isEmpty()); assertFalse(vm.state.value.isSending); assertFalse(vm.state.value.isSubmitting); assertTrue(vm.state.value.errorMessage!!.contains("disponible")) },
            case("rename-trims-and-updates-matching-only") { val vm = aiVm(); ai.give("renameConversation", conversation.copy(title = "Renamed")); vm.renameConversation(1, " Renamed "); drain(); assertEquals(UpdateConversationRequest("Renamed"), ai.args("renameConversation")[1]); assertEquals("Renamed", vm.state.value.conversations[0].title); assertEquals("Title", vm.state.value.conversations[1].title) },
            case("rename-failure-preserves-title") { val vm = aiVm(); ai.fail("renameConversation"); vm.renameConversation(1, "Renamed"); drain(); assertEquals("Title", vm.state.value.conversations[0].title); assertFalse(vm.state.value.isSubmitting) },
            case("delete-selected-clears-history") { val vm = aiVm(); select(vm); ai.give("deleteConversation", Unit); vm.deleteConversation(1); drain(); assertNull(vm.state.value.selectedConversationId); assertTrue(vm.state.value.messages.isEmpty()); assertEquals(listOf(2L), vm.state.value.conversations.map { it.id }) },
            case("delete-other-preserves-selected-history") { val vm = aiVm(); ai.give("getMessages", listOf(message)); vm.selectConversation(1); drain(); ai.give("deleteConversation", Unit); vm.deleteConversation(2); drain(); assertEquals(1L, vm.state.value.selectedConversationId); assertEquals(listOf(message), vm.state.value.messages) },
            case("delete-forbidden-preserves-selection") { val vm = aiVm(); select(vm); ai.fail("deleteConversation", httpError(403)); vm.deleteConversation(1); drain(); assertEquals(1L, vm.state.value.selectedConversationId); assertEquals(2, vm.state.value.conversations.size); assertNotNull(vm.state.value.errorMessage) },
            case("ai-http-local-contract") { localHttp("[{\"id\":2,\"sender\":\"ASSISTANT\",\"content\":\"Reply\",\"created_at\":\"2026-10-05T12:00:00Z\"}]") { r, s -> kotlinx.coroutines.runBlocking { val result = r.create(AiApi::class.java).sendMessage(1, SendMessageRequest("hello", "es")); assertEquals(listOf(message), result); val json = requestJson(s); assertEquals("hello", json["content"].asString); assertEquals("es", json["language"].asString) } } },
        )
    }
}
