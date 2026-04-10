package com.idsr_project.Model

data class ResendOtpRequest(
    val email: String
)

data class VerifyEmailRequest(
    val email: String,
    val otp: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class VerifyResetOtpRequest(
    val email: String,
    val otp: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)

data class OtpResponse(
    val success: Boolean,
    val message: String,
    val requiresVerification: Boolean? = null,
    val email: String? = null
)

data class ProfileData(
    val id: Int,
    val firstname: String,
    val lastname: String,
    val email: String,
    val phone: String?,
    val gender: String?,
    val role: String?,
    val is_verified: Boolean,
    val created_at: String?,
    val region_name: String?,
    val district_name: String?
)

data class ProfileResponse(
    val success: Boolean,
    val data: ProfileData
)

data class UpdateProfileRequest(
    val firstname: String,
    val lastname: String,
    val phone: String?
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)