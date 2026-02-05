package com.idsr_project.Model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializer

data class loginResponse(
    val success: Boolean,
    @SerializedName("message") val msg: String,
    @SerializedName("access_token") val access_token: String? = null,
    @SerializedName("refresh_token") val refresh_token: String? = null,
    val user: User? = null
)
data class User (
    val id: Int,
    val firstname: String,
    val lastname: String,
    val email: String,
    val phone: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("region_id") val regionId: Int?,
    @SerializedName("district_id") val districtId: Int?
)
