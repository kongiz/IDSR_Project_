package com.idsr_project.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.idsr_project.api.ApiClient
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.data.local.DistrictEntity
import com.idsr_project.data.local.FacilityEntity
import com.idsr_project.data.local.RegionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ReferenceDataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "ReferenceDataSyncWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ReferenceDataSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "idsr_reference_sync",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getInstance(applicationContext).referenceDataDao()
            val api = ApiClient.getClient(applicationContext)


            val regionsResponse = api.getRegions().execute()
            if (!regionsResponse.isSuccessful || regionsResponse.body()?.success != true) {
                Log.e(TAG, "Failed to fetch regions")
                return@withContext Result.retry()
            }

            val regions = regionsResponse.body()!!.data ?: emptyList()
            dao.clearRegions()
            dao.insertRegions(regions.map {
                RegionEntity(id = it.region_id, name = it.region_name)
            })
            Log.d(TAG, "Cached ${regions.size} regions")


            dao.clearDistricts()
            val allDistricts = mutableListOf<DistrictEntity>()

            for (region in regions) {
                val districtsResponse = api.getDistrictsByRegion(region.region_id).execute()
                if (districtsResponse.isSuccessful && districtsResponse.body()?.success == true) {
                    val districts = districtsResponse.body()!!.data ?: emptyList()
                    allDistricts.addAll(districts.map {
                        DistrictEntity(
                            id = it.district_id,
                            name = it.district_name,
                            regionId = it.region_id
                        )
                    })
                } else {
                    Log.w(TAG, "Failed to fetch districts for region ${region.region_id}")
                }
            }

            dao.insertDistricts(allDistricts)
            Log.d(TAG, "Cached ${allDistricts.size} districts")


            dao.clearFacilities()
            val allFacilities = mutableListOf<FacilityEntity>()

            for (district in allDistricts) {
                val facilitiesResponse = api.getFacilities(district.id).execute()
                if (facilitiesResponse.isSuccessful && facilitiesResponse.body()?.success == true) {
                    val facilities = facilitiesResponse.body()!!.data ?: emptyList()
                    allFacilities.addAll(facilities.map {
                        FacilityEntity(
                            id = it.facility_id,
                            name = it.facility_name,
                            districtId = it.district_id
                        )
                    })
                } else {
                    Log.w(TAG, "Failed to fetch facilities for district ${district.id}")
                }
            }

            dao.insertFacilities(allFacilities)
            Log.d(TAG, "Cached ${allFacilities.size} facilities")

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "ReferenceDataSyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}