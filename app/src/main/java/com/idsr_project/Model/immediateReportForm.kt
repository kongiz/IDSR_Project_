package com.idsr_project.Model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class immediateReportForm(
    @SerializedName("recordId")              val recordId: String,
    @SerializedName("country")               val country: String,
    @SerializedName("province")              val province: String,
    @SerializedName("district")              val district: Int,
    @SerializedName("site")                  val site: String,
    @SerializedName("disease")               val disease: String,
    @SerializedName("inpatientOutpatient")   val inpatientOutpatient: String,
    @SerializedName("dateSeen")              val dateSeen: String,
    @SerializedName("patientName")           val patientName: String,
    @SerializedName("dateOfBirth")           val dateOfBirth: String,
    @SerializedName("age")                   val age: Int,
    @SerializedName("gender")               val gender: String,
    @SerializedName("address")               val address: String,
    @SerializedName("districtAnnex2")        val districtAnnex2: String,
    @SerializedName("urbanRural")            val urbanRural: String,
    @SerializedName("phoneNumber")           val phoneNumber: String,
    @SerializedName("occupation")            val occupation: String,
    @SerializedName("dateOfOnset")           val dateOfOnset: String,
    @SerializedName("travelHistory")         val travelHistory: String,
    @SerializedName("destination")           val destination: String,
    @SerializedName("vaccineDoses")          val vaccineDoses: String,
    @SerializedName("dateLastVaccine")       val dateLastVaccine: String,
    @SerializedName("dateSpecimen")          val dateSpecimen: String,
    @SerializedName("dateLab")               val dateLab: String,
    @SerializedName("labResults")            val labResults: String,
    @SerializedName("outcome")               val outcome: String,
    @SerializedName("classification")        val classification: String,
    @SerializedName("dateFacilityNotified")  val dateFacilityNotified: String,
    @SerializedName("dateSentDistrict")      val dateSentDistrict: String,
    @SerializedName("reporterName")          val reporterName: String
) : Parcelable