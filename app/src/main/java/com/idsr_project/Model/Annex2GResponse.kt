package com.idsr_project.Model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class Annex2GResponse(
    val success: Boolean,
    val data: Annex2GResponseData
)

data class Annex2GResponseData(
    val role: String?,
    val page: Int?,
    val limit: Int?,
    val total_records: Int?,
    val total_pages: Int?,
    val results: List<Annex2GData>
)

@Parcelize
data class Annex2GData(
    val id: Int,
    val user_id: Int,
    @SerializedName("date_specimen_collect")  val dateSpecimenCollect: String?,
    @SerializedName("suspected_disease")      val suspectedDisease: String?,
    @SerializedName("specimen_type")          val specimenType: String?,
    @SerializedName("specimen_unique_id")     val specimenUniqueID: String?,
    @SerializedName("patient_name")           val patientNameLab: String?,
    @SerializedName("gender")                 val sex: String?,
    val age: String?,
    @SerializedName("date_specimen_sent_lab") val dateSpecimenSentLab: String?,
    @SerializedName("phone_number")           val phoneNumber: String?,
    @SerializedName("email_clinician")        val emailClinician: String?,
    val created_at: String?,
    val full_name: String?,
    val region_name: String?,
    val district_name: String?
) : Parcelable