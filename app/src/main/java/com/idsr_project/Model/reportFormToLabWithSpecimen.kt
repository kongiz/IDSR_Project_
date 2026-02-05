package com.idsr_project.Model

data class reportFormToLabWithSpecimen(
    val dateSpecimenCollect: String,
    val suspectedDisease: String,
    val specimenType: String,
    val specimenUniqueID: String,
    val patientNameLab: String,
    val sex: String,
    val age: String,
    val dateSpecimenSentLab: String,
    val phoneNumber: String,
    val emailClinician: String?

)
