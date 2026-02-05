package com.idsr_project.Model

data class ResponseApi(
    val status: String?,
    val success: Boolean?,
    val msg: String
) {
    fun isSuccess(): Boolean {
        return status == "success" || success == true
    }
}