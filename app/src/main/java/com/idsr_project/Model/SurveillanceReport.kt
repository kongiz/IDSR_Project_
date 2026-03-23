package com.idsr_project.Model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Diseases(
    val name: String,
    var isExpanded: Boolean = false,
    var u5MaleAlive: Int = 0,
    var u5FemaleAlive: Int = 0,
    var a5MaleAlive: Int = 0,
    var a5FemaleAlive: Int = 0,
    var u5MaleDeath: Int = 0,
    var u5FemaleDeath: Int = 0,
    var a5MaleDeath: Int = 0,
    var a5FemaleDeath: Int = 0,
    var totalSamples: Int = 0
): Parcelable

@Parcelize
data class surveillanceData(
    val healthFacility: String,
    val healthRegion: String,
    val district: String,
    val epiweek: String,
    val dateFrom: String,
    val dateTo: String,
    val facilityGeo: String,
    val regionId: Int,
    val districtId: Int,
    val facilityId: Int,
    val totConU5Male: Int,
    val totConU5Female: Int,
    val totConA5Male: Int,
    val totConA5Female: Int,
    val grandTotal: Int,
    val officerComment: String,
    val officerName: String,
    val designation: String,
    val updatedDiseases: ArrayList<Diseases>
): Parcelable


