package com.idsr_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_reports")
data class PendingReportEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val formType: String,           // "SURVEILLANCE", "ANNEX2F", "SPECIMEN", "LAB"
    val reportJson: String,         // full data object serialized as JSON string
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val submittedBy: String = "",
    val regionName: String = "",
    val districtName: String = ""
)
