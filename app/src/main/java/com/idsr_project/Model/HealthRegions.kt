package com.idsr_project.Model

data class HealthRegions(
    val region_id: Int,
    val region_name: String,
    val region_code: String
)

data class HealthDistricts(
    val district_id: Int,
    val district_name: String
)
data class HealthFacilities(
    val facility_id: Int,
    val facility_name: String
)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

