package com.idsr_project.api

import com.idsr_project.Model.AnalyticResponse
import com.idsr_project.Model.Annex2FResponse
import com.idsr_project.Model.Annex2GResponse
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.ChangePasswordRequest
import com.idsr_project.Model.CountResponse
import com.idsr_project.Model.ForgotPasswordRequest
import com.idsr_project.Model.FormRequest
import com.idsr_project.Model.FormResponse
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthFacilities
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.LabReportResponse
import com.idsr_project.Model.LoginRequest
import com.idsr_project.Model.NotificationsResponse
import com.idsr_project.Model.OtpResponse
import com.idsr_project.Model.ProfileResponse
import com.idsr_project.Model.ResendOtpRequest
import com.idsr_project.Model.ResetPasswordRequest
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.UpdateProfileRequest
import com.idsr_project.Model.VerifyEmailRequest
import com.idsr_project.Model.VerifyResetOtpRequest
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.Model.loginResponse
import com.idsr_project.Model.reportFormToLabWithSpecimen
import com.idsr_project.Model.surveillanceData
import com.idsr_project.Model.userSignup
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiServices {


    @POST("signup")
    fun registerUser(@Body user: userSignup): Call<ResponseApi>

    @POST("adminRegister")
    fun registerPrivilegeUser(@Body data: HashMap<String, String>): Call<ResponseApi>

    @POST("login")
    fun loginUser(@Body request: LoginRequest): Call<loginResponse>

    @POST("auth/resend-otp")
    fun resendOtp(@Body body: ResendOtpRequest): Call<OtpResponse>

    @POST("auth/verify-email")
    fun verifyEmail(@Body body: VerifyEmailRequest): Call<OtpResponse>

    @POST("auth/forgot-password")
    fun forgotPassword(@Body body: ForgotPasswordRequest): Call<OtpResponse>

    @POST("auth/verify-reset-otp")
    fun verifyResetOtp(@Body body: VerifyResetOtpRequest): Call<OtpResponse>

    @POST("auth/reset-password")
    fun resetPassword(@Body body: ResetPasswordRequest): Call<OtpResponse>



    @POST("logout")
    fun logout(@Body body: Map<String, String>): Call<ResponseApi>


    @POST("submit_surveillance_report")
    fun submitSurveillanceData(@Body data: surveillanceData): Call<ResponseApi>

    @POST("submit_annex2FImmediateReport")
    fun submitImmediateReportData(@Body data: immediateReportForm?): Call<ResponseApi>

    @POST("submit_annex2GLabSpecimen")
    fun submitLabFormWithspecimen(@Body reportForm: reportFormToLabWithSpecimen?): Call<ResponseApi>

    @Multipart
    @POST("submit_lab_report")
    fun submitLabReport(
        @Part("labName")                        labName: RequestBody,
        @Part("dateLabReceived")                dateLabReceived: RequestBody,
        @Part("specimenCondition")              specimenCondition: RequestBody,
        @Part("testTypesPerformed")             testTypesPerformed: RequestBody,
        @Part("finalLabResult")                 finalLabResult: RequestBody,
        @Part("dateLabSentDistrict")            dateLabSentDistrict: RequestBody,
        @Part("dateDistrictReceivedLabResult")  dateDistrictReceivedLabResult: RequestBody,
        @Part labResultImages: List<MultipartBody.Part>
    ): Call<ResponseApi>

    @GET("get_surveillance_reports")
    fun getSurveillanceReport(
        @Query("page")        page: Int = 1,
        @Query("limit")       limit: Int = 20,
        @Query("search")      search: String? = null,
        @Query("start_date")  startDate: String? = null,
        @Query("end_date")    endDate: String? = null
    ): Call<FormResponse>

    @GET("get_labReports")
    fun getLabReports(
        @Query("page")    page: Int = 1,
        @Query("limit")   limit: Int = 20,
        @Query("search")  search: String? = null
    ): Call<LabReportResponse>


    @GET("get_annex2FImmediateReports")
    fun getImmediateReport(
        @Query("page")    page: Int = 1,
        @Query("limit")   limit: Int = 20,
        @Query("search")  search: String? = null
    ): Call<Annex2FResponse>

    @GET("get_annex2GLabSpecimen")
    fun getAnnex2GLabReports(
        @Query("page")    page: Int = 1,
        @Query("limit")   limit: Int = 20,
        @Query("search")  search: String? = null
    ): Call<Annex2GResponse>



    @GET("get_healthRegions")
    fun getRegions(): Call<ApiResponse<List<HealthRegions>>>

    @GET("get_healthDistricts_by_region")
    fun getDistrictsByRegion(@Query("region_id") regionId: Int): Call<ApiResponse<List<HealthDistricts>>>

    @GET("get_healthFacilities")
    fun getFacilities(@Query("district_id") districtId: Int): Call<ApiResponse<List<HealthFacilities>>>



    @GET("get_reports_count")
    fun getReportCount(
        @Query("role")      role: String,
        @Query("user_id")   userId: Int,
        @Query("region")    regionId: Int?   = null,
        @Query("district")  districtId: Int? = null
    ): Call<CountResponse>

    @GET("get_analytics")
    fun getAnalytics(
        @Query("role")      role: String,
        @Query("user_id")   userId: Int,
        @Query("region")    region: Int?   = null,
        @Query("district")  district: Int? = null
    ): Call<AnalyticResponse>



    @GET("notifications")
    fun getNotifications(): Call<NotificationsResponse>

    @PATCH("notifications/{id}/read")
    fun markNotificationRead(@Path("id") id: Int): Call<ResponseApi>

    @PATCH("notifications/read-all")
    fun markAllNotificationsRead(): Call<ResponseApi>



    @POST("fcm-token")
    fun saveFcmToken(@Body body: Map<String, String>): Call<ResponseApi>


    @GET("profile")
    fun getProfile(): Call<ProfileResponse>

    @PATCH("profile/update")
    fun updateProfile(@Body body: UpdateProfileRequest): Call<ResponseApi>

    @PATCH("profile/change-password")
    fun changePassword(@Body body: ChangePasswordRequest): Call<ResponseApi>
}