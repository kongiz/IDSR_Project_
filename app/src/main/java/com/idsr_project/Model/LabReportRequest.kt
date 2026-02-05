package com.idsr_project.Model

data class LabReportRequest(
    val page: Int = 1,
    val limit: Int = 20,
    val region_id: Int? = null,
    val district_id: Int? = null,
    val search: String? = null,
    val date_lab_received: String? = null,
    val result_type: String? = null
)
