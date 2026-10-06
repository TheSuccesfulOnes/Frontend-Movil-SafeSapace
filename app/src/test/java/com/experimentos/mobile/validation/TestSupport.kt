@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.experimentos.mobile.validation

import android.content.SharedPreferences
import com.experimentos.mobile.activity.data.*
import com.experimentos.mobile.ai.data.*
import com.experimentos.mobile.authentication.data.*
import com.experimentos.mobile.authentication.domain.*
import com.experimentos.mobile.comment.data.*
import com.experimentos.mobile.mood.data.*
import com.experimentos.mobile.profile.data.*
import com.experimentos.mobile.report.data.*
import com.experimentos.mobile.shared.data.*
import com.experimentos.mobile.survey.data.*
import java.lang.reflect.Proxy
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response

class MainDispatcherRule : TestWatcher() {
    val dispatcher = StandardTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) {
        try {
            dispatcher.scheduler.advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

data class Scenario(val name: String, val kind: String, val body: TestEnv.() -> Unit) {
    override fun toString() = "$kind:$name"
}
fun case(name: String, kind: String = "integration", body: TestEnv.() -> Unit) = Scenario(name, kind, body)
fun rows(vararg cases: Scenario): List<Array<Any>> = cases.map { arrayOf(it) }

abstract class ScenarioTest(private val scenario: Scenario) {
    @get:Rule val main = MainDispatcherRule()
    @Test fun scenario() {
        val env = TestEnv(main.dispatcher)
        scenario.body(env)
        env.drain()
        env.verify()
    }
}

/** An explicit, per-case in-memory service. Unexpected calls fail, never reach a provider. */
class ScriptedApi<T : Any>(type: Class<T>) {
    data class Call(val method: String, val args: List<Any?>)
    val calls = mutableListOf<Call>()
    private val answers = mutableMapOf<String, ArrayDeque<Any?>>()
    private data class Failure(val error: Throwable)
    private object Hold
    private val held = mutableMapOf<String, Continuation<Any?>>()
    private val unexpected = mutableListOf<String>()
    val api: T = type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
        if (method.name == "toString") return@newProxyInstance "LocalScriptedApi"
        if (method.name == "hashCode") return@newProxyInstance System.identityHashCode(this)
        val arguments = args.orEmpty().toList()
        calls += Call(method.name, arguments.dropLast(1))
        val queue = answers[method.name]
        if (queue == null || queue.isEmpty()) {
            unexpected += method.name
            error("Unstubbed or exhausted local API call: ${method.name}")
        }
        when (val answer = queue.removeFirst()) {
            is Failure -> {
                @Suppress("UNCHECKED_CAST")
                (arguments.last() as Continuation<Any?>).resumeWith(Result.failure(answer.error))
                COROUTINE_SUSPENDED
            }
            Hold -> {
                @Suppress("UNCHECKED_CAST")
                held[method.name] = arguments.last() as Continuation<Any?>
                COROUTINE_SUSPENDED
            }
            else -> answer
        }
    })
    fun give(method: String, vararg values: Any?) { answers.getOrPut(method) { ArrayDeque() }.addAll(values) }
    fun fail(method: String, error: Throwable = IOException("local failure")) { give(method, Failure(error)) }
    fun hold(method: String) { give(method, Hold) }
    fun release(method: String, value: Any?) { held.remove(method)!!.resumeWith(Result.success(value)) }
    fun reject(method: String, error: Throwable) { held.remove(method)!!.resumeWith(Result.failure(error)) }
    fun count(method: String) = calls.count { it.method == method }
    fun args(method: String) = calls.last { it.method == method }.args
    fun verify() { check(unexpected.isEmpty()) { "Unexpected local API calls: $unexpected" }; check(held.isEmpty()) { "Unreleased local operations: ${held.keys}" } }
}

class MemoryPreferences : SharedPreferences {
    val values = mutableMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String?, defValue: String?) = if (values.containsKey(key)) values[key] as String? else defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST") return if (values.containsKey(key)) (values[key] as Set<String>?)?.toMutableSet() else defValues
    }
    override fun getInt(key: String?, defValue: Int) = if (values.containsKey(key)) values[key] as Int else defValue
    override fun getLong(key: String?, defValue: Long) = if (values.containsKey(key)) values[key] as Long else defValue
    override fun getFloat(key: String?, defValue: Float) = if (values.containsKey(key)) values[key] as Float else defValue
    override fun getBoolean(key: String?, defValue: Boolean) = if (values.containsKey(key)) values[key] as Boolean else defValue
    override fun contains(key: String?) = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        val changes = mutableMapOf<String, Any?>()
        var cleared = false
        override fun putString(key: String?, value: String?) = apply { changes[key!!] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { changes[key!!] = values?.toSet() }
        override fun putInt(key: String?, value: Int) = apply { changes[key!!] = value }
        override fun putLong(key: String?, value: Long) = apply { changes[key!!] = value }
        override fun putFloat(key: String?, value: Float) = apply { changes[key!!] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { changes[key!!] = value }
        override fun remove(key: String?) = apply { changes[key!!] = null }
        override fun clear() = apply { cleared = true }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (cleared) values.clear()
            changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
        }
    }
}

class TestEnv(val dispatcher: TestDispatcher) {
    val prefs = MemoryPreferences()
    val sessions = SessionStore(prefs)
    val appearance = AppearanceStore(MemoryPreferences())
    val auth = ScriptedApi(AuthApi::class.java)
    val surveys = ScriptedApi(SurveyApi::class.java)
    val comments = ScriptedApi(CommentApi::class.java)
    val activities = ScriptedApi(ActivityApi::class.java)
    val ai = ScriptedApi(AiApi::class.java)
    val moods = ScriptedApi(MoodApi::class.java)
    val profiles = ScriptedApi(ProfileApi::class.java)
    val reports = ScriptedApi(ReportApi::class.java)
    val repository get() = DefaultAuthRepository(auth.api)
    fun drain() = dispatcher.scheduler.advanceUntilIdle()
    fun verify() { listOf(auth, surveys, comments, activities, ai, moods, profiles, reports).forEach { it.verify() } }
}

fun httpError(code: Int, body: String = "{}"): HttpException = HttpException(Response.error<Any>(code, body.toResponseBody()))
val authResponse = AuthResponse("local-token", "ana", "Ana", "EMPLOYEE", 7)
val session = Session(7, "local-token", "ana", "Ana", "EMPLOYEE")
val survey = SurveyResponse(1, "Title", "Question", SurveyType.DAILY, "PUBLISHED", true, 0)
val comment = CommentResponse(4, "Content", "2026-10-05T12:00:00Z", 0, true, emptyList())
val activity = ActivityResponse(1, "Title", "Description", "OPEN", listOf(ActivityOption(10, "A", 0, 0.0), ActivityOption(20, "B", 0, 0.0)))
val conversation = ConversationResponse(1, "Title")
val message = MessageResponse(2, "ASSISTANT", "Reply", "2026-10-05T12:00:00Z")
val profile = ProfileResponse(7, "ana", "ana@example.test", "Ana", "EMPLOYEE", "es", "LIGHT")
val report = ReportResponse(1, "Ops", "Title", "Description", "LOW", "NEW", true)
val reportRequest = CreateReportRequest("Ops", "Title", "Description", "LOW", true)
