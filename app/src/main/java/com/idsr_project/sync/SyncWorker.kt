package com.idsr_project.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.Model.reportFormToLabWithSpecimen
import com.idsr_project.Model.surveillanceData
import com.idsr_project.api.ApiClient
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.data.local.PendingReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).pendingReportDao()
        val gson = Gson()

        val pendingReports = dao.getPendingAndFailedReports()

        if (pendingReports.isEmpty()) {
            return@withContext Result.success()
        }

        var shouldRetry = false

        for (report in pendingReports) {
            dao.updateStatus(report.id, "PENDING")

            val result = syncReport(report, gson)

            when (result) {
                SyncResult.SUCCESS -> {
                    dao.updateStatus(report.id, "SYNCED")
                }

                SyncResult.PERMANENT_FAILURE -> {
                    dao.updateStatus(report.id, "FAILED")

                }

                SyncResult.RETRYABLE_FAILURE -> {
                    dao.updateStatus(report.id, "FAILED")
                    shouldRetry = true
                }
            }
        }

        return@withContext if (shouldRetry) Result.retry() else Result.success()
    }

    private fun syncReport(report: PendingReportEntity, gson: Gson): SyncResult {
        return try {
            val api = ApiClient.getClient(context)

            val response = when (report.formType) {

                "SURVEILLANCE" -> {
                    val data = gson.fromJson(report.reportJson, surveillanceData::class.java)
                    api.submitSurveillanceData(data).execute()
                }

                "ANNEX2F" -> {
                    val data = gson.fromJson(report.reportJson, immediateReportForm::class.java)
                    api.submitImmediateReportData(data).execute()
                }

                "SPECIMEN" -> {
                    val data = gson.fromJson(report.reportJson, reportFormToLabWithSpecimen::class.java)
                    api.submitLabFormWithspecimen(data).execute()
                }

                else -> {
                    Log.e("SYNC", "Unknown form type: ${report.formType}")
                    return SyncResult.PERMANENT_FAILURE
                }
            }

            if (response.isSuccessful) {
                Log.d("SYNC_SUCCESS", "Report ID ${report.id} synced successfully")
                SyncResult.SUCCESS
            } else {
                val errorBody = response.errorBody()?.string()
                val code = response.code()

                Log.e("SYNC_ERROR", """
                    Report ID: ${report.id}
                    Type: ${report.formType}
                    Code: $code
                    Error: $errorBody
                """.trimIndent())

                when (code) {
                    401, 403 -> {
                        SyncResult.PERMANENT_FAILURE
                    }
                    else -> {
                        SyncResult.RETRYABLE_FAILURE
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("SYNC_EXCEPTION", "Report ID ${report.id}: ${e.message}", e)
            SyncResult.RETRYABLE_FAILURE
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "idsr_sync",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}

enum class SyncResult {
    SUCCESS,
    PERMANENT_FAILURE,
    RETRYABLE_FAILURE
}