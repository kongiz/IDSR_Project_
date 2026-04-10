package com.idsr_project.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Disease(
    val name: String?,
    val under5_male: Int?,
    val under5_female: Int?,
    val above5_male: Int?,
    val above5_female: Int?,
    val total: Int?
): Parcelable

@Parcelize
data class FormData(
    val id: Int,
    val user_id: Int?,
    val facility_name: String?,
    val region_name: String?,
    val district_name: String?,
    val epiweek: String?,
    val date_from: String?,
    val date_to: String?,
    val facility_geo: String?,
    val tot_con_u5_male: Int?,
    val tot_con_u5_female: Int?,
    val tot_con_a5_male: Int?,
    val tot_con_a5_female: Int?,
    val grand_total: Int?,
    val officer_comment: String?,
    val officer_name: String?,
    val designation: String?,
    val created_at: String?,
    val type: String?,
    val diseases: List<Disease>? = null
): Parcelable

data class FormResponse(
    val success: Boolean,
    val role: String?,
    val page: Int?,
    val limit: Int?,
    val total_records: Int?,
    val total_pages: Int?,
    val data: List<FormData>?
)

data class FormRequest(
    val userId: Int,  
    val role: String,
    val type: String
)
