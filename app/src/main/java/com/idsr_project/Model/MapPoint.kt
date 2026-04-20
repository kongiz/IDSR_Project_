package com.idsr_project.Model

data class MapPoint(
    val id: Int,
    val geo: String,
    val label: String,
    val type: String,          // "SURVEILLANCE" or "ANNEX2F"
    val region_name: String?,
    val district_name: String?,
    // Surveillance extras
    val epiweek: String?,
    val date_from: String?,
    val date_to: String?,
    val facility_name: String?,
    // Annex2F extras
    val patient_name: String?,
    val outcome: String?,
    val gender: String?,
    val age: Int?,
    val date_seen: String?
)

data class MapPointsResponse(
    val success: Boolean,
    val data: MapPointsData
)

data class MapPointsData(
    val surveillance: List<MapPoint>,
    val annex2f: List<MapPoint>
)
