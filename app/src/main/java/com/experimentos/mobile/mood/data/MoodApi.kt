package com.experimentos.mobile.mood.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

enum class Mood { VERY_BAD, BAD, GOOD, VERY_GOOD }

data class SubmitMoodRequest(val mood: Mood)

data class MoodResponse(val mood: Mood, val date: String)

data class MoodSummary(
    val date: String,
    @SerializedName("total_responses")
    val totalResponses: Long,
    val distribution: Map<Mood, Long>,
    @SerializedName("active_employees")
    val activeEmployees: Long = 0,
    @SerializedName("response_rate")
    val responseRate: Int = 0,
)

interface MoodApi {
    @POST("api/v1/mood/today")
    suspend fun submitToday(@Body request: SubmitMoodRequest): MoodResponse

    @GET("api/v1/mood/today")
    suspend fun getToday(): MoodResponse?

    @GET("api/v1/mood/summary")
    suspend fun getSummary(@Query("date") date: String? = null): MoodSummary
}
