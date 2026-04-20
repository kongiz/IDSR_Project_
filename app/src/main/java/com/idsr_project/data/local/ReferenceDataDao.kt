package com.idsr_project.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReferenceDataDao {

    // Regions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(regions: List<RegionEntity>)

    @Query("SELECT * FROM regions ORDER By name ASC")
    suspend fun getAllRegions(): List<RegionEntity>

    @Query("DELETE FROM regions")
    suspend fun clearRegions()

    // Districts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(districts: List<DistrictEntity>)

    @Query("SELECT * FROM districts ORDER BY name ASC")
    suspend fun getAllDistricts(): List<DistrictEntity>

    @Query("SELECT * FROM districts WHERE regionId = :regionId ORDER BY name ASC")
    suspend fun getDistrictsByRegion(regionId: Int): List<DistrictEntity>

    @Query("DELETE FROM districts")
    suspend fun clearDistricts()

    // Facilities
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilities(facilities: List<FacilityEntity>)

    @Query("SELECT * FROM facilities ORDER BY name ASC")
    suspend fun getAllFacilities(): List<FacilityEntity>

    @Query("SELECT * FROM facilities WHERE districtId = :districtId ORDER BY name ASC")
    suspend fun getFacilitiesByDistrict(districtId: Int): List<FacilityEntity>

    @Query("DELETE FROM facilities")
    suspend fun clearFacilities()

}