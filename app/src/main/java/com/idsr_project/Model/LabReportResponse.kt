package com.idsr_project.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class LabReportResponse(
    val status: String,
    val data: List<LabReportData>
)
@Parcelize
data class LabReportData(
    val id: Int,
    val user_id: Int,
    val lab_name: String?,
    val date_lab_received: String?,
    val specimen_condition: String?,
    val test_types_performed: String?,
    val final_lab_result: String?,
    val date_lab_sent_district: String?,
    val date_district_received_lab_result: String?,
    val lab_result_image: String?,
    val created_at: String?,
    val full_name: String?,
    val region_name: String?,
    val district_name: String?
): Parcelable
