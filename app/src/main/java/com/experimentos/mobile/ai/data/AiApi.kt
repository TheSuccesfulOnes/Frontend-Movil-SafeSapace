package com.experimentos.mobile.ai.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class ConversationResponse(val id: Long, val title: String)

data class UpdateConversationRequest(val title: String)

data class SendMessageRequest(
    val content: String,
    val language: String,
)

data class MessageResponse(
    val id: Long,
    val sender: String,
    val content: String,
    @SerializedName("created_at") val createdAt: String,
)

interface AiApi {
    @POST("api/v1/ai/conversations")
    suspend fun createConversation(): ConversationResponse

    @GET("api/v1/ai/conversations")
    suspend fun getConversations(): List<ConversationResponse>

    @PATCH("api/v1/ai/conversations/{id}")
    suspend fun renameConversation(
        @Path("id") id: Long,
        @Body request: UpdateConversationRequest,
    ): ConversationResponse

    @DELETE("api/v1/ai/conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: Long)

    @GET("api/v1/ai/conversations/{id}/messages")
    suspend fun getMessages(@Path("id") id: Long): List<MessageResponse>

    @POST("api/v1/ai/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: Long,
        @Body request: SendMessageRequest,
    ): List<MessageResponse>
}
