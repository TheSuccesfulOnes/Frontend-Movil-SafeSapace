package com.experimentos.mobile.validation

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** The URL is owned by this fixture; a fail-closed interceptor blocks every other host. */
fun <R> localHttp(body: String = "{}", code: Int = 200, configure: OkHttpClient.Builder.() -> Unit = {}, block: (Retrofit, MockWebServer) -> R): R {
    val server = MockWebServer()
    server.start()
    val client = OkHttpClient.Builder().addInterceptor { chain ->
        check(chain.request().url.host == server.url("/").host && chain.request().url.port == server.port) {
            "External provider calls are forbidden in tests"
        }
        chain.proceed(chain.request())
    }.callTimeout(3, TimeUnit.SECONDS).apply(configure).build()
    try {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
        return block(Retrofit.Builder().baseUrl(server.url("/")).client(client).addConverterFactory(GsonConverterFactory.create()).build(), server)
    } finally {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
        server.shutdown()
    }
}

fun requestJson(server: MockWebServer) = JsonParser.parseString(server.takeRequest(2, TimeUnit.SECONDS)!!.body.readUtf8()).asJsonObject
const val AUTH_JSON = """{"token":"local-token","username":"ana","display_name":"Ana","role":"EMPLOYEE","user_id":7}"""
const val REPORT_JSON = """{"id":1,"category":"Ops","title":"Title","description":"Description","priority":"LOW","status":"NEW","anonymous":true}"""
const val PROFILE_JSON = """{"user_id":7,"username":"ana","email":"ana@example.test","display_name":"Ana","role":"EMPLOYEE","language":"es","theme":"LIGHT"}"""
