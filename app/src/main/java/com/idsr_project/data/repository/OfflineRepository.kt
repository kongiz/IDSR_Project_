package com.idsr_project.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.Model.reportFormToLabWithSpecimen
import com.idsr_project.Model.surveillanceData
import com.idsr_project.api.ApiClient
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.data.local.PendingReportEntity
import com.idsr_project.sync.SyncWorker
import com.idsr_project.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class SubmitResult {
    data class SyncedOnline(val message: String) : SubmitResult()
    object SavedOffline : SubmitResult()
    data class Error(val message: String) : SubmitResult()
}

class OfflineRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).pendingReportDao()
    private val gson = Gson()

    suspend fun submitReport(formType: String, reportJson: String): SubmitResult {

        val rowId = withContext(Dispatchers.IO) {
            dao.insert(
                PendingReportEntity(
                    formType     = formType,
                    reportJson   = reportJson,
                    submittedBy  = SessionManager.getFullName(context),
                    regionName   = SessionManager.getUserRegion(context) ?: "",
                    districtName = SessionManager.getUserDistrict(context) ?: ""
                )
            )
        }

        return if (isOnline()) {
            trySyncNow(rowId, formType, reportJson)
        } else {
            SyncWorker.schedule(context)
            SubmitResult.SavedOffline
        }
    }

    private suspend fun trySyncNow(
        rowId: Long,
        formType: String,
        reportJson: String
    ): SubmitResult = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.getClient(context)

            val response = when (formType) {
                "SURVEILLANCE" -> {
                    val data = gson.fromJson(reportJson, surveillanceData::class.java)
                    Log.d("ANNEX2F_JSON", reportJson)
                    api.submitSurveillanceData(data).execute()
                }
                "ANNEX2F" -> {
                    val data = gson.fromJson(reportJson, immediateReportForm::class.java)
                    Log.d("ANNEX2F_PAYLOAD", "Sending JSON: $reportJson")
                    api.submitImmediateReportData(data).execute()
                }
                "SPECIMEN" -> {
                    val data = gson.fromJson(reportJson, reportFormToLabWithSpecimen::class.java)
                    api.submitLabFormWithspecimen(data).execute()
                }
                else -> null
            }

            if (response != null && response.isSuccessful && response.body() != null) {
                dao.updateStatus(rowId, "SYNCED")
                SubmitResult.SyncedOnline(
                    response.body()!!.msg ?: "Submitted successfully"
                )
            } else {
                val errorBody = response?.errorBody()?.string() ?: "null body"
                val code      = response?.code() ?: -1
                Log.e("SYNC_ERROR", "Code: $code | Body: $errorBody")
                Log.d("SYNC_PAYLOAD", "Sending: $reportJson")
                dao.updateStatus(rowId, "PENDING")
                SyncWorker.schedule(context)
                SubmitResult.SavedOffline
            }

        } catch (e: Exception) {
            Log.e("SYNC_EXCEPTION", "Exception type: ${e.javaClass.simpleName} | Message: ${e.message}", e)
            dao.updateStatus(rowId, "PENDING")
            SyncWorker.schedule(context)
            SubmitResult.SavedOffline
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}