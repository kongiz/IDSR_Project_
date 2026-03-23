package com.idsr_project.sync

import android.content.Context
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
        val dao      = AppDatabase.getInstance(context).pendingReportDao()
        val gson     = Gson()
        val pending  = dao.getPendingReports().filter { it.formType == "LAB" }

        if (pending.isEmpty()) return Result.success()

        val uploader = LabUploader(context)
        var hadFailure = false

        for (report in pending) {
            val data   = gson.fromJson(report.reportJson, LabFormOfflineData::class.java)
            val synced = uploader.tryUpload(data)

            if (synced) {
                dao.updateStatus(report.id, "SYNCED")
            } else {
                dao.updateStatus(report.id, "FAILED")
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