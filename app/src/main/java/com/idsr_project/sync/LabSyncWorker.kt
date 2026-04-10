package com.idsr_project.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.idsr_project.activities.Laboratory_Form_2_Annex2G_Activity.LabFormOfflineData
import com.idsr_project.data.local.AppDatabase
import java.util.concurrent.TimeUnit

class LabSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(context).pendingReportDao()
        val gson = Gson()

        val pending = dao.getPendingReports().filter { it.formType == "LAB" && it.status != "SYNCED" }

        if (pending.isEmpty()) return Result.success()

        val uploader = LabUploader(context)
        var hadFailure = false

        for (report in pending) {
            try {
                val data = gson.fromJson(report.reportJson, LabFormOfflineData::class.java)
                val synced = uploader.tryUpload(data)

                if (synced) {
                    dao.updateStatus(report.id, "SYNCED")
                    Log.d("LAB_SYNC", "Report ${report.id} synced successfully")
                } else {
                    dao.updateStatus(report.id, "FAILED")
                    hadFailure = true
                }
            } catch (e: Exception) {
                Log.e("LAB_SYNC", "Error parsing/uploading report ${report.id}", e)
                hadFailure = true
            }
        }


        return if (hadFailure) Result.retry() else Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<LabSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "idsr_lab_sync",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}