package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.idsr_project.databinding.ActivityLaboratoryForm2Annex2GactivityBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.labFormData
import com.idsr_project.api.ApiClient
import com.idsr_project.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File

class Laboratory_Form_2_Annex2G_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityLaboratoryForm2Annex2GactivityBinding
    private val calendar = Calendar.getInstance()


    private lateinit var imgPreview: ImageView
    private lateinit var uploadBtn: MaterialButton
    private var photoUri: Uri? = null


    private var galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imgPreview.visibility = View.VISIBLE
            imgPreview.setImageURI(it)
            photoUri = it
        }
    }
    private var cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && photoUri != null) {
            imgPreview.visibility = View.VISIBLE
            imgPreview.setImageURI(photoUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLaboratoryForm2Annex2GactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackAnnex2G2.setOnClickListener {
            finish()
        }

        imgPreview = binding.imgLabResultPreview
        uploadBtn = binding.btnUploadLabResult


        uploadBtn.setOnClickListener {
            showImagePickerDialog()
        }
        binding.rgSpecimenCon.setOnCheckedChangeListener { group, checkedId ->
            val selected = findViewById<RadioButton>(checkedId)
            selected?.let {
                val specimen = it.text.toString()
                Toast.makeText(this, "Specimen: $specimen", Toast.LENGTH_SHORT).show()
            }
        }
        binding.etDateLabReceived.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateLabReceived.setText(dateString)
            }
        }
        binding.etDateLabSentDistrict.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateLabSentDistrict.setText(dateString)
            }

        }
        binding.etDateDistrictReceivedLabResult.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateDistrictReceivedLabResult.setText(dateString)
            }
        }
        binding.btnLabTech.setOnClickListener {
            if (validateLabFormForLabTech2()) {
                val formData = collectFormData()
                uploadLabReport(formData)
            }
        }

    }
    private fun showImagePickerDialog() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(this)
            .setTitle("Upload Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> galleryLauncher.launch("image/*")
                    1 -> checkCameraPermission()
                }
            }
            .show()



    }
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST)
        } else {
            openCamera()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun openCamera() {
        val values = ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.TITLE, "Lab Result")
            put(android.provider.MediaStore.Images.Media.DESCRIPTION, "Photo taken for lab result")
        }
        photoUri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        photoUri?.let { cameraLauncher.launch(it) }
    }
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onDateSelected(dateFormat.format(selectedCal.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
    private fun getRealPathFromUri(uri: Uri): File {
        var tempFilePath = ""
        lateinit var tempFile : File
        try {
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("gif") -> "gif"
                else -> "jpg" // default to jpg for safety
            }

            val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open input stream for URI.")
           tempFile = File(cacheDir, "temp_upload_${System.currentTimeMillis()}.$extension")

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            tempFilePath = tempFile.absolutePath
            Log.d("FILE_TEMP", "Temp file created: $tempFilePath, Size: ${tempFile.length()}")

        } catch (e: Exception) {
            Log.e("FILE_TEMP_ERROR", "Error converting URI to temp file: ${e.message}", e)
        }
        return tempFile
    }
    private fun validateLabFormForLabTech2(): Boolean {
        var isValid = true

        if (binding.etLabName.text.isNullOrEmpty()) {
            binding.etLabName.error = "Lab name is required"
            isValid = false
        } else {
            binding.tilLabName.error = null
        }
        if (binding.etDateLabReceived.text.isNullOrEmpty()) {
            binding.etDateLabReceived.error = "Date laboratory received is required"
            isValid = false
        } else {
            binding.tilDateLabReceived.error = null
        }
        if (binding.rgSpecimenCon.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Please select specimen condition", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (binding.etTestTypesPerformed.text.isNullOrEmpty()) {
            binding.etTestTypesPerformed.error = "Test types performed is required"
            isValid = false
        } else {
            binding.tilTestTypesPerformed.error = null
        }
        if (binding.etFinalLAbResult.text.isNullOrEmpty()) {
            binding.etFinalLAbResult.error = "Final laboratory result is required"
            isValid = false
        } else {
            binding.tilFinalLabResult.error = null
        }
        if (binding.etDateLabSentDistrict.text.isNullOrEmpty()) {
            binding.etDateLabSentDistrict.error = "Date laboratory sent to district is required"
            isValid = false
        } else {
            binding.tilDateLabSentDistrict.error = null
        }
        if (binding.etDateDistrictReceivedLabResult.text.isNullOrEmpty()) {
            binding.etDateDistrictReceivedLabResult.error = "Date district received laboratory result is required"
            isValid = false
        } else {
            binding.tilDateDistrictReceivedLabResult.error = null
        }
        if (photoUri == null && imgPreview.drawable == null) {
            Toast.makeText(this, "Please upload lab result image", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        return isValid
    }
    private fun collectFormData(): labFormData {
        val labName = binding.etLabName.text.toString()
        val dateLabReceived = binding.etDateLabReceived.text.toString()
        val specimenCondition = binding.rgSpecimenCon.findViewById<RadioButton>(binding.rgSpecimenCon.checkedRadioButtonId).text.toString()
        val testTypesPerformed = binding.etTestTypesPerformed.text.toString()
        val finalLabResult = binding.etFinalLAbResult.text.toString()
        val dateLabSentDistrict = binding.etDateLabSentDistrict.text.toString()
        val dateDistrictReceivedLabResult = binding.etDateDistrictReceivedLabResult.text.toString()
        val labResultImage = photoUri

        return labFormData(
            labName,
            dateLabReceived,
            specimenCondition,
            testTypesPerformed,
            finalLabResult,
            dateLabSentDistrict,
            dateDistrictReceivedLabResult,
            labResultImage
        )
    }
    private fun uploadLabReport(formData: labFormData) {
        val labNamePart = formData.labName.toRequestBody("text/plain".toMediaTypeOrNull())
        val dateLabReceivedPart = formData.dateLabReceived.toRequestBody("text/plain".toMediaTypeOrNull())
        val specimenConditionPart = formData.specimenCondition.toRequestBody("text/plain".toMediaTypeOrNull())
        val testTypesPerformedPart = formData.testTypesPerformed.toRequestBody("text/plain".toMediaTypeOrNull())
        val finalLabResultPart = formData.finalLabResult.toRequestBody("text/plain".toMediaTypeOrNull())
        val dateLabSentDistrictPart = formData.dateLabSentDistrict.toRequestBody("text/plain".toMediaTypeOrNull())
        val dateDistrictReceivedLabResultPart = formData.dateDistrictReceivedLabResult.toRequestBody("text/plain".toMediaTypeOrNull())

        var imagePart:  MultipartBody.Part? = null
        formData.labResultImage?.let { uri ->
            val file = getRealPathFromUri(uri)
            Log.d("UPLOAD_DEBUG", "File path: ${getRealPathFromUri(uri)}")
            val requestFile = file.asRequestBody("image/jpg".toMediaTypeOrNull())

            imagePart = MultipartBody.Part.createFormData("labResultImage", file.name, requestFile)
        }
        val call = ApiClient.getClient(context = this).submitLabReport(
            labNamePart,
            dateLabReceivedPart,
            specimenConditionPart,
            testTypesPerformedPart,
            finalLabResultPart,
            dateLabSentDistrictPart,
            dateDistrictReceivedLabResultPart,
            imagePart

        )
        call.enqueue(object : retrofit2.Callback<ResponseApi> {
            override fun onResponse(
                call: Call<ResponseApi?>,
                response: Response<ResponseApi?>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    if (responseBody.status == "success") {
                        Toast.makeText(this@Laboratory_Form_2_Annex2G_Activity, responseBody.msg, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Laboratory_Form_2_Annex2G_Activity, Success_Activity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@Laboratory_Form_2_Annex2G_Activity,
                            "Server Error: ${responseBody}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e(" My Error for server","Server Error: ${responseBody.msg}")
                    }
                } else {
                    Toast.makeText(
                        this@Laboratory_Form_2_Annex2G_Activity,
                        "Failed to submit form",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            override fun onFailure(call: Call<ResponseApi?>, t: Throwable) {
                Toast.makeText(
                    this@Laboratory_Form_2_Annex2G_Activity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}