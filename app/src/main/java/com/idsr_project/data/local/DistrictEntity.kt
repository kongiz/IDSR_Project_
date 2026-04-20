package com.idsr_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "districts")
data class DistrictEntity (
    @PrimaryKey
    val id: Int,
    val name: String,
    val regionId: Int

)