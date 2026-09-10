package com.experimentos.mobile.shared.data

import com.experimentos.mobile.activity.data.ActivityApi
import com.experimentos.mobile.ai.data.AiApi
import com.experimentos.mobile.BuildConfig
import com.experimentos.mobile.authentication.data.AuthApi
import com.experimentos.mobile.comment.data.CommentApi
import com.experimentos.mobile.mood.data.MoodApi
import com.experimentos.mobile.profile.data.ProfileApi
import com.experimentos.mobile.profile.data.ProfilePhotoStore
import com.experimentos.mobile.report.data.ReportApi
import com.experimentos.mobile.survey.data.SurveyApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private class AccessTokenInterceptor(private val sessionStore: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.encodedPath.startsWith("/api/v1/auth/")) {
            return chain.proceed(chain.request())
        }

        val token = sessionStore.currentToken()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        val response = chain.proceed(request)
        if (response.code == 401) {
            sessionStore.clearImmediately()
        }
        return response
    }
}

/** Centralizes HTTP configuration so every feature uses the same security defaults. */
class AppContainer(context: android.content.Context) {
    /**
     * The application-wide session source shared by the UI and the HTTP client.
     * Keeping one instance prevents authentication state from becoming stale.
     */
    val sessionStore = SessionStore(context)
    val appearanceStore = AppearanceStore(context)
    val profilePhotoStore = ProfilePhotoStore(context)
    private val client = OkHttpClient.Builder()
        .addInterceptor(AccessTokenInterceptor(sessionStore))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val moodApi: MoodApi = retrofit.create(MoodApi::class.java)
    val profileApi: ProfileApi = retrofit.create(ProfileApi::class.java)
    val surveyApi: SurveyApi = retrofit.create(SurveyApi::class.java)
    val activityApi: ActivityApi = retrofit.create(ActivityApi::class.java)
    val commentApi: CommentApi = retrofit.create(CommentApi::class.java)
    val reportApi: ReportApi = retrofit.create(ReportApi::class.java)
    val aiApi: AiApi = retrofit.create(AiApi::class.java)
}
