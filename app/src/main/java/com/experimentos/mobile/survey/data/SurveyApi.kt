package com.experimentos.mobile.survey.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

enum class SurveyType { DAILY, WEEKLY }

data class SurveyResponse(
    val id: Long,
    val title: String,
    val question: String,
    val type: SurveyType,
    val status: String,
    @SerializedName("allow_comments") val allowComments: Boolean,
    val answers: Long,
    /** Indicates whether the authenticated user has already answered this survey. */
    val answered: Boolean = false,
)

data class AnswerRequest(@SerializedName("answer_text") val answerText: String)

data class CreateSurveyRequest(
    val title: String,
    val question: String,
    val type: SurveyType,
    @SerializedName("allow_comments") val allowComments: Boolean,
)

interface SurveyApi {
    @GET("api/v1/surveys")
    suspend fun getPublished(): List<SurveyResponse>

    @GET("api/v1/surveys/managed")
    suspend fun getManaged(): List<SurveyResponse>

    @POST("api/v1/surveys")
    suspend fun create(@Body request: CreateSurveyRequest): SurveyResponse

    @POST("api/v1/surveys/{id}/publish")
    suspend fun publish(@Path("id") id: Long): SurveyResponse

    @POST("api/v1/surveys/{id}/close")
    suspend fun close(@Path("id") id: Long): SurveyResponse

    @POST("api/v1/surveys/{id}/answers")
    suspend fun answer(@Path("id") id: Long, @Body request: AnswerRequest)
}
