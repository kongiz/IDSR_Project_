package com.idsr_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regions")
data class RegionEntity(
    @PrimaryKey
    val id: Int,
    val name: String

)
