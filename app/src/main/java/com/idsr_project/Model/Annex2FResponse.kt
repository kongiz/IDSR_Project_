package com.idsr_project.Model

import android.R
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class Annex2FResponse(
    val success: Boolean,
    val data: List<Annex2FData>

)
@Parcelize
data class Annex2FData(
    val id: Int,
    val user_id: Int,
    val recordId: String?,
    val country: String?,
    val province: String?,
    val district: String?,
    val site: String?,
    val disease: String?,
    val inpatientOutpatient: String?,
    val dateSeen: String?,
    val patientName: String?,
    val dateOfBirth: String?,
    val age: String?,
    val gender: String?,
    val address: String?,
    val districtAnnex2: String?,
    val urbanRural: String?,
    val phoneNumber: String?,
    val occupation: String?,
    val dateOfOnset: String?,
    val travelHistory: String?,
    val destination: String?,
    val vaccineDoses: String?,
    val dateLastVaccine: String?,
    val dateSpecimen: String?,
    val dateLab: String?,
    val labResults: String?,
    val outcome: String?,
    val classification: String?,
    val dateFacilityNotified: String?,
    val dateSentDistrict: String?,
    val reporterName: String?,
    val created_at: String?,
    val full_name: String?,
    val region_name: String?,
    val district_name: String?
): Parcelable
