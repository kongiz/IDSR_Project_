package com.idsr_project.Model

import com.google.gson.annotations.SerializedName

data class AdminUser(
    val id: Int,
    val firstname: String,
    val lastname: String,
    val email: String,
    val phone: String?,
    @SerializedName("role")
    val user_role: String,
    val is_active: Boolean,
    val is_verified: Boolean,
    val created_at: String?,
    val region_name: String?,
    val district_name: String?
)

data class AdminUsersResponse(
    val success: Boolean,
    val data: List<AdminUser>?
)

data class UpdateStatusRequest(
    val is_active: Boolean
)

data class UpdateRoleRequest(
    val role: String
)

data class UpdateUserResponse(
    val success: Boolean,
    val message: String?,
    val data: AdminUser?
)