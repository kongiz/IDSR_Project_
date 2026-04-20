package com.idsr_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facilities")
data class FacilityEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val districtId: Int
)
