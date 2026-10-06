package com.experimentos.mobile.validation

import com.experimentos.mobile.shared.data.*
import com.experimentos.mobile.profile.data.ProfilePhotoStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SessionStoreValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            corrupt("missing-token", "access_token", null), corrupt("empty-token", "access_token", ""),
            corrupt("blank-token", "access_token", " \t"), corrupt("missing-username", "username", null),
            corrupt("empty-username", "username", ""), corrupt("blank-username", "username", "\n"),
            corrupt("missing-role", "role", null), corrupt("empty-role", "role", ""), corrupt("blank-role", "role", " "),
            case("missing-display-name-falls-back", "unit") { seed(); prefs.values.remove("display_name"); assertEquals("ana", SessionStore(prefs).session.value!!.displayName) },
            case("blank-display-name-falls-back", "unit") { seed(); prefs.values["display_name"] = " "; assertEquals("ana", SessionStore(prefs).session.value!!.displayName) },
            case("save-and-recreate") { runBlocking { sessions.save(session) }; assertEquals(session, SessionStore(prefs).session.value); assertEquals("local-token", sessions.currentToken()) },
            case("legacy-id-is-absent") { runBlocking { sessions.save(session.copy(userId = null)) }; assertFalse(prefs.contains("user_id")); assertEquals("legacy:ana", SessionStore(prefs).session.value!!.accountKey) },
            case("legacy-overwrite-removes-old-id") { runBlocking { sessions.save(session); sessions.save(session.copy(userId = null)) }; assertNull(SessionStore(prefs).session.value!!.userId) },
            case("immutable-id-survives-rename") { runBlocking { sessions.save(session); sessions.updateAccount("new-token", "bea", "Bea") }; val updated = SessionStore(prefs).session.value!!; assertEquals("user:7", updated.accountKey); assertEquals("bea", updated.username); assertEquals("EMPLOYEE", updated.role) },
            case("update-without-session-no-op") { runBlocking { sessions.updateAccount("token", "bea", "Bea") }; assertNull(sessions.currentToken()); assertTrue(prefs.values.isEmpty()) },
            case("update-replaces-token-and-id") { runBlocking { sessions.save(session); sessions.updateAccount("replacement", "bea", "Bea", 8) }; assertEquals("replacement", sessions.currentToken()); assertEquals("user:8", SessionStore(prefs).session.value!!.accountKey) },
            case("clear-removes-all-claims") { runBlocking { sessions.save(session); sessions.clear() }; assertTrue(prefs.values.isEmpty()); assertNull(SessionStore(prefs).session.value); assertNull(sessions.currentToken()) },
            case("immediate-clear-observable") { runBlocking { sessions.save(session) }; sessions.clearImmediately(); assertNull(sessions.session.value); assertTrue(prefs.values.isEmpty()) },
            case("clear-is-idempotent") { sessions.clearImmediately(); sessions.clearImmediately(); assertNull(sessions.session.value); assertNull(SessionStore(prefs).session.value) },
        )
        private fun corrupt(name: String, key: String, value: String?) = case(name, "unit") {
            seed(); if (value == null) prefs.values.remove(key) else prefs.values[key] = value
            assertNull(SessionStore(prefs).session.value)
        }
    }
}
private fun TestEnv.seed() { prefs.values.putAll(mapOf("access_token" to "local-token", "username" to "ana", "role" to "EMPLOYEE", "display_name" to "Ana", "user_id" to 7L)) }

@RunWith(Parameterized::class)
class AppearanceStoreValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            theme("dark", "DARK", "DARK"), theme("case-insensitive-dark", "dark", "DARK"), theme("light", "LIGHT", "LIGHT"),
            theme("unknown-theme-default", "SYSTEM", "LIGHT"), theme("blank-theme-default", "", "LIGHT"), theme("padded-theme-default", " DARK ", "LIGHT"),
            language("english", "en", "en"), language("uppercase-english", "EN", "en"), language("spanish", "es", "es"),
            language("unknown-language-default", "fr", "es"), language("blank-language-default", " ", "es"), language("padded-language-default", " en ", "es"),
            case("account-round-trip") { appearance.activateAccount("user:7"); appearance.saveTheme("DARK"); appearance.saveLanguage("en"); appearance.activateAccount(null); appearance.activateAccount("user:7"); assertEquals("DARK", appearance.theme.value); assertEquals("en", appearance.language.value) },
            case("cross-account-isolation") { appearance.activateAccount("user:7"); appearance.saveTheme("DARK"); appearance.saveLanguage("en"); appearance.activateAccount("user:8"); assertEquals("LIGHT", appearance.theme.value); assertEquals("es", appearance.language.value) },
            case("signout-defaults") { appearance.activateAccount("user:7"); appearance.saveTheme("DARK"); appearance.activateAccount(null); assertEquals("LIGHT", appearance.theme.value); assertEquals("es", appearance.language.value) },
            case("blank-account-is-unscoped") { appearance.activateAccount(" "); appearance.saveTheme("DARK"); appearance.activateAccount(" "); assertEquals("LIGHT", appearance.theme.value) },
            case("signed-out-changes-not-persisted") { appearance.saveTheme("DARK"); appearance.saveLanguage("en"); appearance.activateAccount(null); assertEquals("LIGHT", appearance.theme.value); assertEquals("es", appearance.language.value) },
            case("server-sync-normalizes-and-persists") { appearance.syncAccountPreferences("user:7", "EN", "dark"); appearance.activateAccount(null); appearance.activateAccount("user:7"); assertEquals("DARK", appearance.theme.value); assertEquals("en", appearance.language.value) },
            case("server-sync-invalid-defaults") { appearance.syncAccountPreferences("user:7", "fr", "SYSTEM"); assertEquals("LIGHT", appearance.theme.value); assertEquals("es", appearance.language.value) },
            case("corrupt-persisted-values-default") { val p = MemoryPreferences(); p.values["account_user:7_theme"] = "INVALID"; p.values["account_user:7_language"] = "INVALID"; val store = AppearanceStore(p); store.activateAccount("user:7"); assertEquals("LIGHT", store.theme.value); assertEquals("es", store.language.value) },
        )
        private fun theme(name: String, input: String, expected: String) = case(name, "unit") { appearance.saveTheme(input); assertEquals(expected, appearance.theme.value) }
        private fun language(name: String, input: String, expected: String) = case(name, "unit") { appearance.saveLanguage(input); assertEquals(expected, appearance.language.value) }
    }
}

@RunWith(Parameterized::class)
class PhotoStoreValidationTest(scenario: Scenario) : ScenarioTest(scenario) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = rows(
            removed("null-removes", null), removed("empty-removes", ""), removed("space-removes", " "),
            removed("tab-removes", "\t"), removed("newline-removes", "\n"), removed("unicode-space-removes", "\u2003"),
            alias("username-case-normalized", "ANA", "ana"), alias("username-leading-space", " ana", "ana"),
            alias("username-trailing-space", "ana ", "ana"), alias("username-newline-trimmed", "\nana\n", "ana"),
            alias("username-unicode-case", "ÁRBOL", "árbol"), alias("username-period-retained", "ana.b", "ana.b"),
            case("uri-round-trip") { val s = ProfilePhotoStore(prefs); s.saveUri("ana", "content://photos/1"); assertEquals("content://photos/1", ProfilePhotoStore(prefs).getUri("ana")) },
            case("account-isolation") { val s = ProfilePhotoStore(prefs); s.saveUri("ana", "content://photos/1"); assertNull(s.getUri("bea")) },
            case("deletion-isolated") { val s = ProfilePhotoStore(prefs); s.saveUri("ana", "one"); s.saveUri("bea", "two"); s.saveUri("ana", null); assertEquals("two", s.getUri("bea")); assertNull(s.getUri("ana")) },
            case("overwrite-existing-photo") { val s = ProfilePhotoStore(prefs); s.saveUri("ana", "one"); s.saveUri("ANA", "two"); assertEquals("two", s.getUri("ana")); assertEquals(1, prefs.values.size) },
            case("missing-account-null") { assertNull(ProfilePhotoStore(prefs).getUri("ana")) },
            case("uri-is-not-normalized") { val s = ProfilePhotoStore(prefs); s.saveUri("ana", " content://photos/1 "); assertEquals(" content://photos/1 ", s.getUri("ana")) },
            case("deletion-is-idempotent") { val s = ProfilePhotoStore(prefs); s.saveUri("ana", null); s.saveUri("ana", ""); assertTrue(prefs.values.isEmpty()) },
            case("normalization-does-not-collapse-internal-spaces") { val s = ProfilePhotoStore(prefs); s.saveUri("a na", "one"); assertNull(s.getUri("ana")); assertEquals("one", s.getUri("a na")) },
        )
        private fun removed(name: String, uri: String?) = case(name) { val s = ProfilePhotoStore(prefs); s.saveUri("ana", "content://photos/1"); s.saveUri("ana", uri); assertNull(s.getUri("ana")); assertTrue(prefs.values.isEmpty()) }
        private fun alias(name: String, write: String, read: String) = case(name) { val s = ProfilePhotoStore(prefs); s.saveUri(write, "content://photos/1"); assertEquals("content://photos/1", s.getUri(read)) }
    }
}
