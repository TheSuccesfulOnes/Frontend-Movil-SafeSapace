package com.experimentos.mobile.activity.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class ActivityOption(val id: Long, val label: String, val votes: Long, val percentage: Double)

data class ActivityResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val options: List<ActivityOption>,
)

data class VoteRequest(@SerializedName("option_id") val optionId: Long?)

data class CreateActivityRequest(
    val title: String,
    val description: String?,
    val options: List<String>,
)

interface ActivityApi {
    @GET("api/v1/activities")
    suspend fun getOpen(): List<ActivityResponse>

    @GET("api/v1/activities/managed")
    suspend fun getManaged(): List<ActivityResponse>

    @POST("api/v1/activities")
    suspend fun create(@Body request: CreateActivityRequest): ActivityResponse

    @POST("api/v1/activities/{id}/close")
    suspend fun close(@Path("id") id: Long)

    @POST("api/v1/activities/{id}/votes")
    suspend fun vote(@Path("id") id: Long, @Body request: VoteRequest)
}
