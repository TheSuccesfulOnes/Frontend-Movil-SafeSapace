package com.experimentos.mobile.validation

import com.experimentos.mobile.activity.presentation.*
import com.experimentos.mobile.activity.data.*
import com.experimentos.mobile.mood.presentation.*
import com.experimentos.mobile.mood.data.*
import java.io.EOFException
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private fun TestEnv.activityVm(): ActivityViewModel { activities.give("getOpen", listOf(activity)); return ActivityViewModel(activities.api).also { drain() } }

@RunWith(Parameterized::class)
class ActivityViewModelValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            case("unknown-activity-rejected", "unit") { val vm = activityVm(); vm.choose(2, 10); assertTrue(vm.state.value.pendingOptions.isEmpty()) },
            case("closed-activity-rejected", "unit") { activities.give("getOpen", listOf(activity.copy(status = "CLOSED"))); val vm = ActivityViewModel(activities.api); drain(); vm.choose(1, 10); assertTrue(vm.state.value.pendingOptions.isEmpty()) },
            case("unknown-option-rejected", "unit") { val vm = activityVm(); vm.choose(1, 99); assertTrue(vm.state.value.pendingOptions.isEmpty()) },
            case("option-of-other-activity-rejected", "unit") { activities.give("getOpen", listOf(activity, activity.copy(id = 2, options = listOf(ActivityOption(99, "X", 0, 0.0))))); val vm = ActivityViewModel(activities.api); drain(); vm.choose(1, 99); assertTrue(vm.state.value.pendingOptions.isEmpty()) },
            case("empty-options-rejected", "unit") { activities.give("getOpen", listOf(activity.copy(options = emptyList()))); val vm = ActivityViewModel(activities.api); drain(); vm.choose(1, 10); assertTrue(vm.state.value.pendingOptions.isEmpty()) },
            case("vote-without-selection-no-api", "unit") { val vm = activityVm(); vm.vote(1); drain(); assertEquals(0, activities.count("vote")) },
            case("vote-other-activity-no-api", "unit") { val vm = activityVm(); vm.choose(1, 10); vm.vote(2); drain(); assertEquals(0, activities.count("vote")); assertEquals(10L, vm.state.value.pendingOptions[1]) },
            case("choose-first-option") { val vm = activityVm(); vm.choose(1, 10); assertEquals(10L, vm.state.value.pendingOptions[1]); assertTrue(vm.state.value.selectedOptions.isEmpty()) },
            case("choose-replaces-pending") { val vm = activityVm(); vm.choose(1, 10); vm.choose(1, 20); assertEquals(20L, vm.state.value.pendingOptions[1]) },
            case("invalid-choice-preserves-valid-pending") { val vm = activityVm(); vm.choose(1, 10); vm.choose(1, 99); assertEquals(10L, vm.state.value.pendingOptions[1]) },
            case("load-list-success") { val vm = activityVm(); assertEquals(listOf(activity), vm.state.value.activities); assertFalse(vm.state.value.isLoading) },
            case("load-empty-is-valid") { activities.give("getOpen", emptyList<ActivityResponse>()); val vm = ActivityViewModel(activities.api); drain(); assertTrue(vm.state.value.activities.isEmpty()); assertNull(vm.state.value.errorMessage) },
            case("load-network-failure") { activities.fail("getOpen"); val vm = ActivityViewModel(activities.api); drain(); assertFalse(vm.state.value.isLoading); assertNotNull(vm.state.value.errorMessage) },
            case("reload-after-error") { activities.fail("getOpen"); val vm = ActivityViewModel(activities.api); drain(); activities.give("getOpen", listOf(activity)); vm.load(); drain(); assertNull(vm.state.value.errorMessage); assertEquals(1, vm.state.value.activities.size) },
            case("vote-success-refreshes-aggregates") { val vm = activityVm(); activities.give("vote", Unit); activities.give("getOpen", listOf(activity.copy(options = listOf(ActivityOption(10, "A", 1, 100.0))))); vm.choose(1, 10); vm.vote(1); drain(); assertEquals(VoteRequest(10), activities.args("vote")[1]); assertEquals(10L, vm.state.value.selectedOptions[1]); assertTrue(vm.state.value.pendingOptions.isEmpty()); assertEquals(1L, vm.state.value.activities[0].options[0].votes) },
            case("change-vote-success") { val vm = activityVm(); activities.give("vote", Unit, Unit); activities.give("getOpen", listOf(activity), listOf(activity)); vm.choose(1, 10); vm.vote(1); drain(); vm.choose(1, 20); vm.vote(1); drain(); assertEquals(20L, vm.state.value.selectedOptions[1]); assertEquals(2, activities.count("vote")) },
            case("vote-forbidden-retains-pending") { val vm = activityVm(); activities.fail("vote", httpError(403)); vm.choose(1, 10); vm.vote(1); drain(); assertEquals(10L, vm.state.value.pendingOptions[1]); assertTrue(vm.state.value.selectedOptions.isEmpty()); assertFalse(vm.state.value.isSubmitting); assertNotNull(vm.state.value.errorMessage) },
            case("vote-retry-after-failure") { val vm = activityVm(); activities.fail("vote"); activities.give("vote", Unit); activities.give("getOpen", listOf(activity)); vm.choose(1, 10); vm.vote(1); drain(); vm.vote(1); drain(); assertNull(vm.state.value.errorMessage); assertEquals(10L, vm.state.value.selectedOptions[1]) },
            case("vote-refresh-failure-preserves-success") { val vm = activityVm(); activities.give("vote", Unit); activities.fail("getOpen"); vm.choose(1, 10); vm.vote(1); drain(); assertEquals(10L, vm.state.value.selectedOptions[1]); assertNotNull(vm.state.value.message); assertNull(vm.state.value.errorMessage) },
            case("vote-loading-lifecycle") { val vm = activityVm(); activities.hold("vote"); activities.give("getOpen", listOf(activity)); vm.choose(1, 10); vm.vote(1); drain(); assertTrue(vm.state.value.isSubmitting); activities.release("vote", Unit); drain(); assertFalse(vm.state.value.isSubmitting) },
        )
    }
}

@RunWith(Parameterized::class)
class HomeViewModelValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            loadMood("load-very-bad", Mood.VERY_BAD), loadMood("load-bad", Mood.BAD), loadMood("load-good", Mood.GOOD), loadMood("load-very-good", Mood.VERY_GOOD),
            submitMood("submit-very-bad", Mood.VERY_BAD), submitMood("submit-bad", Mood.BAD), submitMood("submit-good", Mood.GOOD), submitMood("submit-very-good", Mood.VERY_GOOD),
            case("no-mood-null-valid") { moods.give("getToday", null); val vm = HomeViewModel(moods.api); drain(); assertNull(vm.state.value.selectedMood); assertNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isLoading) },
            case("empty-200-eof-valid") { moods.fail("getToday", EOFException()); val vm = HomeViewModel(moods.api); drain(); assertNull(vm.state.value.errorMessage); assertNull(vm.state.value.selectedMood) },
            case("network-failure-is-error") { moods.fail("getToday"); val vm = HomeViewModel(moods.api); drain(); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isLoading) },
            case("unauthorized-is-error") { moods.fail("getToday", httpError(401)); val vm = HomeViewModel(moods.api); drain(); assertTrue(vm.state.value.errorMessage!!.contains("sesión")) },
            case("load-pending-state") { moods.hold("getToday"); val vm = HomeViewModel(moods.api); drain(); assertTrue(vm.state.value.isLoading); moods.release("getToday", null); drain(); assertFalse(vm.state.value.isLoading) },
            case("retry-load-clears-error") { moods.fail("getToday"); val vm = HomeViewModel(moods.api); drain(); moods.give("getToday", MoodResponse(Mood.GOOD, "2026-10-05")); vm.loadToday(); drain(); assertNull(vm.state.value.errorMessage); assertEquals(Mood.GOOD, vm.state.value.selectedMood) },
            case("submit-failure-retains-previous-mood") { moods.give("getToday", MoodResponse(Mood.BAD, "2026-10-05")); moods.fail("submitToday"); val vm = HomeViewModel(moods.api); drain(); vm.submitMood(Mood.GOOD); drain(); assertEquals(Mood.BAD, vm.state.value.selectedMood); assertNotNull(vm.state.value.errorMessage); assertFalse(vm.state.value.isSubmitting) },
            case("submit-uses-server-mood") { moods.give("getToday", null); moods.give("submitToday", MoodResponse(Mood.VERY_GOOD, "2026-10-05")); val vm = HomeViewModel(moods.api); drain(); vm.submitMood(Mood.GOOD); drain(); assertEquals(Mood.VERY_GOOD, vm.state.value.selectedMood) },
            case("submit-pending-state") { moods.give("getToday", null); moods.hold("submitToday"); val vm = HomeViewModel(moods.api); drain(); vm.submitMood(Mood.GOOD); drain(); assertTrue(vm.state.value.isSubmitting); moods.release("submitToday", MoodResponse(Mood.GOOD, "2026-10-05")); drain(); assertFalse(vm.state.value.isSubmitting) },
            case("submit-retry-after-failure") { moods.give("getToday", null); moods.fail("submitToday"); moods.give("submitToday", MoodResponse(Mood.GOOD, "2026-10-05")); val vm = HomeViewModel(moods.api); drain(); vm.submitMood(Mood.GOOD); drain(); vm.submitMood(Mood.GOOD); drain(); assertNull(vm.state.value.errorMessage); assertEquals(Mood.GOOD, vm.state.value.selectedMood) },
            case("reload-day-rollover-is-server-driven") { moods.give("getToday", MoodResponse(Mood.GOOD, "2026-10-05"), null); val vm = HomeViewModel(moods.api); drain(); vm.loadToday(); drain(); assertNull(vm.state.value.selectedMood); assertNull(vm.state.value.errorMessage) },
            case("mood-http-serialization") { localHttp("{\"mood\":\"VERY_BAD\",\"date\":\"2026-10-05\"}") { r, s -> kotlinx.coroutines.runBlocking { val response = r.create(MoodApi::class.java).submitToday(SubmitMoodRequest(Mood.VERY_BAD)); assertEquals(Mood.VERY_BAD, response.mood); assertEquals("VERY_BAD", requestJson(s)["mood"].asString) } } },
        )
        private fun loadMood(name: String, mood: Mood) = case(name) { moods.give("getToday", MoodResponse(mood, "2026-10-05")); val vm = HomeViewModel(moods.api); drain(); assertEquals(mood, vm.state.value.selectedMood); assertNull(vm.state.value.errorMessage) }
        private fun submitMood(name: String, mood: Mood) = case(name) { moods.give("getToday", null); moods.give("submitToday", MoodResponse(mood, "2026-10-05")); val vm = HomeViewModel(moods.api); drain(); vm.submitMood(mood); drain(); assertEquals(SubmitMoodRequest(mood), moods.args("submitToday")[0]); assertEquals(mood, vm.state.value.selectedMood) }
    }
}
