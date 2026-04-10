package com.idsr_project.Model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class Annex2FResponse(
    val success: Boolean,
    val role: String?,
    val page: Int?,
    val limit: Int?,
    val total_records: Int?,
    val total_pages: Int?,
    val data: List<Annex2FData>?
)

@Parcelize
data class Annex2FData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    val user_id: Int,

    // Backend returns "reporting_site_name", not "site"
    @SerializedName("reporting_site_name")
    val site: String?,

    @SerializedName("disease")
    val disease: String?,

    // Backend returns "patient_type", not "inpatientOutpatient"
    @SerializedName("patient_type")
    val inpatientOutpatient: String?,

    @SerializedName("date_seen")
    val dateSeen: String?,

    @SerializedName("patient_name")
    val patientName: String?,

    @SerializedName("date_of_birth")
    val dateOfBirth: String?,

    @SerializedName("age")
    val age: String?,

    @SerializedName("gender")
    val gender: String?,

    @SerializedName("address")
    val address: String?,

    // Backend returns "area", not "urbanRural"
    @SerializedName("area")
    val urbanRural: String?,

    @SerializedName("phone_number")
    val phoneNumber: String?,

    @SerializedName("occupation")
    val occupation: String?,

    @SerializedName("date_of_onset")
    val dateOfOnset: String?,

    @SerializedName("travel_history")
    val travelHistory: String?,

    @SerializedName("destination")
    val destination: String?,

    @SerializedName("vaccine_doses")
    val vaccineDoses: String?,

    @SerializedName("date_last_vaccine")
    val dateLastVaccine: String?,

    @SerializedName("date_specimen")
    val dateSpecimen: String?,

    @SerializedName("date_lab")
    val dateLab: String?,

    @SerializedName("lab_results")
    val labResults: String?,

    @SerializedName("outcome")
    val outcome: String?,

    @SerializedName("classification")
    val classification: String?,

    @SerializedName("date_facility_notified")
    val dateFacilityNotified: String?,

    @SerializedName("date_sent_district")
    val dateSentDistrict: String?,

    @SerializedName("reporter_name")
    val reporterName: String?,

    @SerializedName("created_at")
    val created_at: String?,

    // Backend returns "submitted_by", not "full_name"
    @SerializedName("submitted_by")
    val full_name: String?,

    @SerializedName("region_name")
    val region_name: String?,

    @SerializedName("district_name")
    val district_name: String?,

    // district_id in case you need it
    @SerializedName("district_id")
    val district_id: Int?

) : Parcelable