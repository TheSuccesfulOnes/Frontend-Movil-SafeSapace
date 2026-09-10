package com.experimentos.mobile.comment.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class CommentResponse(
    val id: Long,
    val content: String,
    @SerializedName("created_at") val createdAt: String,
    val likes: Long,
    val replies: List<CommentResponse>,
)

data class CreateCommentRequest(val content: String, val parentId: Long?)

interface CommentApi {
    @GET("api/v1/surveys/{surveyId}/comments")
    suspend fun getComments(@Path("surveyId") surveyId: Long): List<CommentResponse>

    @POST("api/v1/surveys/{surveyId}/comments")
    suspend fun create(
        @Path("surveyId") surveyId: Long,
        @Body request: CreateCommentRequest,
    ): CommentResponse

    @POST("api/v1/surveys/{surveyId}/comments/{commentId}/like")
    suspend fun like(
        @Path("surveyId") surveyId: Long,
        @Path("commentId") commentId: Long,
    )
}
