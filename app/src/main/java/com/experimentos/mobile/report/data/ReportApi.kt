package com.experimentos.mobile.report.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class CreateReportRequest(
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val anonymous: Boolean,
)

data class UpdateReportStatusRequest(val status: String)

data class ReportResponse(
    val id: Long,
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val anonymous: Boolean,
    @SerializedName("reporter_display_name") val reporterDisplayName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

interface ReportApi {
    @POST("api/v1/reports")
    suspend fun create(@Body request: CreateReportRequest): ReportResponse

    @GET("api/v1/reports/mine")
    suspend fun getMine(): List<ReportResponse>

    @GET("api/v1/reports")
    suspend fun getAll(): List<ReportResponse>

    @PATCH("api/v1/reports/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Long,
        @Body request: UpdateReportStatusRequest,
    ): ReportResponse
}
