package com.idsr_project.Model

import com.google.gson.annotations.SerializedName

data class ResponseApi(
    val status: String?,
    val success: Boolean?,
    @SerializedName("message") val msg: String? = null
) {
    fun isSuccess(): Boolean {
        return status == "success" || success == true
    }
}