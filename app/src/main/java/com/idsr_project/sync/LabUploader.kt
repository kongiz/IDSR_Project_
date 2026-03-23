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
            val file = File(data.imagePath)
            if (!file.exists()) {
                Log.e("LAB_UPLOAD", "Image file not found: ${data.imagePath}")
                return false
            }

            val imagePart = MultipartBody.Part.createFormData(
                "labResultImage",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )

            val response = ApiClient.getClient(context).submitLabReport(
                labName                       = data.labName.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabReceived               = data.dateLabReceived.toRequestBody("text/plain".toMediaTypeOrNull()),
                specimenCondition             = data.specimenCondition.toRequestBody("text/plain".toMediaTypeOrNull()),
                testTypesPerformed            = data.testTypesPerformed.toRequestBody("text/plain".toMediaTypeOrNull()),
                finalLabResult                = data.finalLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabSentDistrict           = data.dateLabSentDistrict.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateDistrictReceivedLabResult = data.dateDistrictReceivedLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                labResultImage                = imagePart
            ).execute()

            response.isSuccessful
        } catch (e: Exception) {
            Log.e("LAB_UPLOAD", "Upload failed: ${e.message}", e)
            false
        }
    }
}