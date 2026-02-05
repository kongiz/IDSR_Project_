package com.idsr_project.Model

import android.net.Uri

data class labFormData(
   val labName: String,
    val dateLabReceived: String,
    val specimenCondition: String,
    val testTypesPerformed: String,
    val finalLabResult: String,
    val dateLabSentDistrict: String,
    val dateDistrictReceivedLabResult: String,
    val labResultImage: Uri?

)
