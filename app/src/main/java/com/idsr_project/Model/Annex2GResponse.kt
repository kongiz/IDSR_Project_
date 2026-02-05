package com.idsr_project.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class Annex2GResponse(
    val status: String,
    val data: List<Annex2GData>

)
@Parcelize
data class Annex2GData(
    val id: Int,
    val user_id: Int,
    val dateSpecimenCollect: String?,
    val suspectedDisease: String?,
    val specimenType: String?,
    val specimenUniqueID: String?,
    val patientNameLab: String?,
    val sex: String?,
    val age: String?,
    val dateSpecimenSentLab: String?,
    val phoneNumber: String?,
    val emailClinician: String?,
    val created_at: String?,
    val full_name: String?,
    val region_name: String?,
    val district_name: String?
): Parcelable
