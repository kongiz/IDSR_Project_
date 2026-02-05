package com.idsr_project.Model

data class Annex2GRequest(
    val page: Int = 1,
    val limit: Int = 20,
    val region_id: Int? = null,
    val district_id: Int? = null,
    val search: String? = null,
    val date_Specimen_Collect: String? = null
)
