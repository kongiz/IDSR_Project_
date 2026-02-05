package com.idsr_project.Model

data class HealthRegions(
    val regions_id: Int,
    val region_name: String,
    val region_code: String
)

data class HealthDistricts(
    val district_id: Int,
    val district_name: String
)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

