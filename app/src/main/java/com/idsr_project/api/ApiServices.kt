package com.idsr_project.api

import com.idsr_project.Model.AnalyticResponse
import com.idsr_project.Model.Annex2FResponse
import com.idsr_project.Model.Annex2GResponse
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.CountResponse
import com.idsr_project.Model.FormRequest
import com.idsr_project.Model.FormResponse
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.LabReportResponse
import com.idsr_project.Model.LoginRequest
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.Model.loginResponse
import com.idsr_project.Model.reportFormToLabWithSpecimen
import com.idsr_project.Model.surveillanceData
import com.idsr_project.Model.userSignup
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiServices {


    @POST("api/signup")
    fun registerUser(@Body user: userSignup): Call<ResponseApi>
    @POST("api/adminRegister")
    fun registerPrivilegeUser(@Body data: HashMap<String, String>): Call<ResponseApi>


    @POST("api/login")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<loginResponse>


    @Headers("Content-Type: application/json")
    @POST("api/submit_surveillance_report")
    fun submitSurveillanceData(@Body data: surveillanceData): Call<ResponseApi>


    @POST("api/submit_annex2FImmediateReport")
    fun submitImmediateReportData(@Body data: immediateReportForm?): Call<ResponseApi>

    @POST("api/submit_annex2GLabSpecimen")
    fun submitLabFormWithspecimen(@Body reportForm: reportFormToLabWithSpecimen?): Call<ResponseApi>

    @Multipart
    @POST("api/submit_lab_report")
    fun submitLabReport(
        @Part("labName") labName: RequestBody,
        @Part("dateLabReceived") dateLabReceived: RequestBody,
        @Part("specimenCondition") specimenCondition: RequestBody,
        @Part("testTypesPerformed") testTypesPerformed: RequestBody,
        @Part("finalLabResult") finalLabResult: RequestBody,
        @Part("dateLabSentDistrict") dateLabSentDistrict: RequestBody,
        @Part("dateDistrictReceivedLabResult") dateDistrictReceivedLabResult: RequestBody,
        @Part labResultImage: MultipartBody.Part?
    ): Call<ResponseApi>


    @GET("api/get_healthRegions")
    fun getRegions(): Call<ApiResponse<List<HealthRegions>>>

    @GET("api/get_healthDistricts_by_region/:region_id")
    fun getDistrictsByRegion(@Query("regions_id") regionId: Int): Call<ApiResponse<List<HealthDistricts>>>



    @POST("api/get_surveillance_reports")
    fun getSurveillanceReport(@Body request: FormRequest): Call<FormResponse>

    @POST("api/get_labReports")
    fun getLabReports(@Header("Authorization") authToken: String?): Call<LabReportResponse>

    @POST("api/get_annex2FImmediateReports")
    fun getImmediateReport(@Header("Authorization") authToken: String?): Call<Annex2FResponse>

    @POST("api/get_annex2GLabSpecimen")
    fun getAnnex2GLabReports(@Header("Authorization") authToken: String?): Call<Annex2GResponse>


    @GET("api/get_reports_count")
    fun getReportCount(
        @Query("role") role: String,
        @Query("user_id") userId: Int,
        @Query("region") regionId: Int? = null,
        @Query("district") districtId: Int? = null
    ): Call<CountResponse>


    @GET("api/get_analytics")
    fun getAnalytics(
        @Query("role") role: String,
        @Query("user_id") userId: Int,
        @Query("region") region: Int? = null,
        @Query("district") district: Int? = null
    ): Call<AnalyticResponse>
}