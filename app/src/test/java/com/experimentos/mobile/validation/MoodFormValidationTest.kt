package com.experimentos.mobile.validation

import com.experimentos.mobile.mood.presentation.*
import com.experimentos.mobile.mood.data.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MoodFormValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            gate("no-mood-ready", null, false, true), gate("no-mood-submitting", null, true, false),
            gate("very-bad-recorded", Mood.VERY_BAD, false, false), gate("bad-recorded", Mood.BAD, false, false),
            gate("good-recorded", Mood.GOOD, false, false), gate("very-good-recorded", Mood.VERY_GOOD, false, false),
            gate("recorded-and-busy", Mood.GOOD, true, false),
            loaded("loaded-very-bad-locks-form", Mood.VERY_BAD), loaded("loaded-bad-locks-form", Mood.BAD),
            loaded("loaded-good-locks-form", Mood.GOOD), loaded("loaded-very-good-locks-form", Mood.VERY_GOOD),
            submit("very-bad-form-flow", Mood.VERY_BAD), submit("bad-form-flow", Mood.BAD), submit("good-form-flow", Mood.GOOD), submit("very-good-form-flow", Mood.VERY_GOOD),
            case("first-day-null-enables-form") { moods.give("getToday", null); val vm = HomeViewModel(moods.api); drain(); assertTrue(canRecordMood(vm.state.value.selectedMood, vm.state.value.isSubmitting)) },
            case("empty-200-enables-form") { moods.fail("getToday", java.io.EOFException()); val vm = HomeViewModel(moods.api); drain(); assertTrue(canRecordMood(vm.state.value.selectedMood, vm.state.value.isSubmitting)); assertNull(vm.state.value.errorMessage) },
            case("submit-pending-blocks-another-selection") { moods.give("getToday", null); moods.hold("submitToday"); val vm = HomeViewModel(moods.api); drain(); vm.submitMood(Mood.GOOD); drain(); assertFalse(canRecordMood(vm.state.value.selectedMood, vm.state.value.isSubmitting)); moods.release("submitToday", MoodResponse(Mood.GOOD, "2026-10-05")); drain(); assertFalse(canRecordMood(vm.state.value.selectedMood, vm.state.value.isSubmitting)) },
            case("submit-failure-enables-retry") { moods.give("getToday", null); moods.fail("submitToday"); val vm = HomeViewModel(moods.api); drain(); vm.submitMood(Mood.GOOD); drain(); assertTrue(canRecordMood(vm.state.value.selectedMood, vm.state.value.isSubmitting)); assertNotNull(vm.state.value.errorMessage) },
            case("day-rollover-server-reload-unlocks-form") { moods.give("getToday", MoodResponse(Mood.GOOD, "2026-10-05"), null); val vm = HomeViewModel(moods.api); drain(); assertFalse(canRecordMood(vm.state.value.selectedMood, false)); vm.loadToday(); drain(); assertTrue(canRecordMood(vm.state.value.selectedMood, false)) },
        )
        private fun gate(name: String, selected: Mood?, busy: Boolean, expected: Boolean) = case(name, "unit") { assertEquals(expected, canRecordMood(selected, busy)) }
        private fun loaded(name: String, mood: Mood) = case(name) { moods.give("getToday", MoodResponse(mood, "2026-10-05")); val vm = HomeViewModel(moods.api); drain(); assertFalse(canRecordMood(vm.state.value.selectedMood, vm.state.value.isSubmitting)); assertEquals(0, moods.count("submitToday")) }
        private fun submit(name: String, mood: Mood) = case(name) { moods.give("getToday", null); moods.give("submitToday", MoodResponse(mood, "2026-10-05")); val vm = HomeViewModel(moods.api); drain(); assertTrue(canRecordMood(vm.state.value.selectedMood, false)); vm.submitMood(mood); drain(); assertEquals(SubmitMoodRequest(mood), moods.args("submitToday")[0]); assertFalse(canRecordMood(vm.state.value.selectedMood, vm.state.value.isSubmitting)) }
    }
}
