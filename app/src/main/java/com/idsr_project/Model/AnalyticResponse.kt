package com.idsr_project.Model

data class AnalyticResponse(
    val success: Boolean,
    val data: AnalyticData
)

data class AnalyticData(
    val totals: ReportTotals,
    val weekly_trend: List<TrendItem>?,
    val top_diseases: List<DiseaseItem>?,
    val gender_distribution: GenderDistribution?,
    val age_groups: AgeGroups?,
    val top_facilities: List<FacilityItem>?,
    val lab_turnaround: LabTurnaround?
)

data class ReportTotals(
    val surveillance: Int,
    val immediate: Int,
    val lab: Int,
    val specimen: Int
)

data class TrendItem(
    val epiweek: String,
    val total: Int
)

data class DiseaseItem(
    val disease_name: String,
    val total: Int
)

data class GenderDistribution(
    val male: Int,
    val female: Int
)

data class AgeGroups(
    val u5: Int,
    val g6_15: Int,
    val g16_30: Int,
    val g31_60: Int,
    val above60: Int
)

data class FacilityItem(
    val facility_name: String,  // ← was health_facility
    val total: Int
)

data class LabTurnaround(
    val avg_delay: Double?
)