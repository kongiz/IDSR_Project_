package com.idsr_project.sync

import android.content.Context
import android.util.Log
import com.idsr_project.activities.Laboratory_Form_2_Annex2G_Activity.LabFormOfflineData
import com.idsr_project.api.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class LabUploader(private val context: Context) {

    fun tryUpload(data: LabFormOfflineData): Boolean {
        return try {

            val imageParts = data.imagePaths.mapNotNull { path ->
                val file = File(path)
                if (!file.exists()) {
                    Log.e("LAB_UPLOAD", "Image file missing: $path")
                    return@mapNotNull null
                }

                val mimeType = when {
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    file.name.endsWith(".gif", ignoreCase = true) -> "image/gif"
                    else -> "image/jpeg"
                }

                MultipartBody.Part.createFormData(
                    "labResultImage",
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
            }

            if (imageParts.isEmpty()) {
                Log.e("LAB_UPLOAD", "No valid images — marking as unrecoverable")
                return false
            }

            val response = ApiClient.getClient(context).submitLabReport(
                labName                       = data.labName.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabReceived               = data.dateLabReceived.toRequestBody("text/plain".toMediaTypeOrNull()),
                specimenCondition             = data.specimenCondition.toRequestBody("text/plain".toMediaTypeOrNull()),
                testTypesPerformed            = data.testTypesPerformed.toRequestBody("text/plain".toMediaTypeOrNull()),
                finalLabResult                = data.finalLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabSentDistrict           = data.dateLabSentDistrict.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateDistrictReceivedLabResult = data.dateDistrictReceivedLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                labResultImages               = imageParts
            ).execute()

            response.isSuccessful
        } catch (e: Exception) {
            Log.e("LAB_UPLOAD", "Upload failed: ${e.message}", e)
            false
        }
    }
}