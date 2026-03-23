package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.data.local.PendingReportEntity
import com.idsr_project.data.repository.OfflineRepository
import com.idsr_project.data.repository.SubmitResult
import com.idsr_project.databinding.ActivityLaboratoryForm2Annex2GactivityBinding
import com.idsr_project.sync.LabSyncWorker
import com.idsr_project.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.idsr_project.api.ApiClient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Laboratory_Form_2_Annex2G_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLaboratoryForm2Annex2GactivityBinding
    private val calendar = Calendar.getInstance()
    private lateinit var imgPreview: ImageView
    private lateinit var uploadBtn: MaterialButton
    private var photoUri: Uri? = null
    private val repository by lazy { OfflineRepository(this) }

    // ── data class to hold all form fields + image path for JSON storage ───────
    data class LabFormOfflineData(
        val labName: String,
        val dateLabReceived: String,
        val specimenCondition: String,
        val testTypesPerformed: String,
        val finalLabResult: String,
        val dateLabSentDistrict: String,
        val dateDistrictReceivedLabResult: String,
        val imagePath: String   // absolute file path — not a URI
    )

    private var galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.cardImagePreview.visibility = View.VISIBLE
            imgPreview.setImageURI(it)
            photoUri = it
            Toast.makeText(this, "Image selected from gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private var cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && photoUri != null) {
            binding.cardImagePreview.visibility = View.VISIBLE
            imgPreview.setImageURI(photoUri)
            Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLaboratoryForm2Annex2GactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imgPreview = binding.imgLabResultPreview
        uploadBtn  = binding.btnUploadLabResult

        setupDropdowns()
        setupFieldListeners()
        setupDatePickers()
        setupClickListeners()
    }

    private fun setupDropdowns() {
        val specimenConditions = arrayOf("Adequate", "Not Adequate")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, specimenConditions)
        binding.spinnerSpecimenCon.setAdapter(adapter)
    }

    private fun setupDatePickers() {
        binding.etDateLabReceived.setOnClickListener {
            showDatePicker { binding.etDateLabReceived.setText(it) }
        }
        binding.etDateLabSentDistrict.setOnClickListener {
            showDatePicker { binding.etDateLabSentDistrict.setText(it) }
        }
        binding.etDateDistrictReceivedLabResult.setOnClickListener {
            showDatePicker { binding.etDateDistrictReceivedLabResult.setText(it) }
        }
    }

    private fun setupClickListeners() {
        binding.btnBackAnnex2G2.setOnClickListener { finish() }

        uploadBtn.setOnClickListener { showImagePickerDialog() }

        binding.btnLabTech.setOnClickListener {
            if (validateLabFormForLabTech2()) {
                binding.btnLabTech.isEnabled = false
                binding.btnLabTech.text = "Saving..."
                submitLabForm2()
            }
        }
    }

    private fun setupFieldListeners() {
        val fields = listOf(
            binding.tilLabName                          to binding.etLabName,
            binding.tilDateLabReceived                  to binding.etDateLabReceived,
            binding.tilTestTypesPerformed               to binding.etTestTypesPerformed,
            binding.tilFinalLabResult                   to binding.etFinalLAbResult,
            binding.tilDateLabSentDistrict              to binding.etDateLabSentDistrict,
            binding.tilDateDistrictReceivedLabResult    to binding.etDateDistrictReceivedLabResult
        )
        for ((layout, editText) in fields) {
            editText.addTextChangedListener {
                if (!it.isNullOrEmpty()) layout.error = null
            }
        }
        binding.spinnerSpecimenCon.addTextChangedListener {
            if (!it.isNullOrEmpty()) binding.tilSpecimenCon.error = null
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val year  = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day   = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d)
            onDateSelected(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time))
        }, year, month, day).show()
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Upload Image")
            .setItems(arrayOf("Gallery", "Camera")) { _, which ->
                when (which) {
                    0 -> galleryLauncher.launch("image/*")
                    1 -> checkCameraPermission()
                }
            }.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
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

    private fun validateLabFormForLabTech2(): Boolean {
        var isValid = true

        if (binding.etLabName.text.isNullOrEmpty()) {
            binding.tilLabName.error = "Lab name is required"
            isValid = false
        } else binding.tilLabName.error = null

        if (binding.etDateLabReceived.text.isNullOrEmpty()) {
            binding.tilDateLabReceived.error = "Date laboratory received is required"
            isValid = false
        } else binding.tilDateLabReceived.error = null

        if (binding.spinnerSpecimenCon.text.isNullOrEmpty()) {
            binding.tilSpecimenCon.error = "Specimen condition is required"
            isValid = false
        } else binding.tilSpecimenCon.error = null

        if (binding.etTestTypesPerformed.text.isNullOrEmpty()) {
            binding.tilTestTypesPerformed.error = "Test types performed is required"
            isValid = false
        } else binding.tilTestTypesPerformed.error = null

        if (binding.etFinalLAbResult.text.isNullOrEmpty()) {
            binding.tilFinalLabResult.error = "Final laboratory result is required"
            isValid = false
        } else binding.tilFinalLabResult.error = null

        if (binding.etDateLabSentDistrict.text.isNullOrEmpty()) {
            binding.tilDateLabSentDistrict.error = "Date laboratory sent to district is required"
            isValid = false
        } else binding.tilDateLabSentDistrict.error = null

        if (binding.etDateDistrictReceivedLabResult.text.isNullOrEmpty()) {
            binding.tilDateDistrictReceivedLabResult.error = "Date district received laboratory result is required"
            isValid = false
        } else binding.tilDateDistrictReceivedLabResult.error = null

        if (photoUri == null) {
            Toast.makeText(this, "Please upload lab result image", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    // ── core submit function ───────────────────────────────────────────────────
    private fun submitLabForm2() {
        lifecycleScope.launch {

            // Step 1 — copy image to permanent app storage so it survives offline
            val permanentFile = withContext(Dispatchers.IO) {
                copyImageToPermanentStorage(photoUri!!)
            }

            if (permanentFile == null) {
                Toast.makeText(
                    this@Laboratory_Form_2_Annex2G_Activity,
                    "Failed to process image. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                binding.btnLabTech.isEnabled = true
                binding.btnLabTech.text = "Submit Lab Form"
                return@launch
            }

            // Step 2 — build the offline data object with the permanent file path
            val offlineData = LabFormOfflineData(
                labName                      = binding.etLabName.text.toString().trim(),
                dateLabReceived              = binding.etDateLabReceived.text.toString().trim(),
                specimenCondition            = binding.spinnerSpecimenCon.text.toString().trim(),
                testTypesPerformed           = binding.etTestTypesPerformed.text.toString().trim(),
                finalLabResult               = binding.etFinalLAbResult.text.toString().trim(),
                dateLabSentDistrict          = binding.etDateLabSentDistrict.text.toString().trim(),
                dateDistrictReceivedLabResult = binding.etDateDistrictReceivedLabResult.text.toString().trim(),
                imagePath                    = permanentFile.absolutePath
            )

            val json = Gson().toJson(offlineData)

            // Step 3 — always save to Room first
            val dao = AppDatabase.getInstance(this@Laboratory_Form_2_Annex2G_Activity).pendingReportDao()
            val rowId = dao.insert(
                PendingReportEntity(
                    formType    = "LAB",
                    reportJson  = json,
                    submittedBy = SessionManager.getFullName(this@Laboratory_Form_2_Annex2G_Activity) ?: ""
                )
            )

            // Step 4 — try immediate upload if online
            if (repository.isOnline()) {
                val synced = withContext(Dispatchers.IO) {
                    tryUploadLabNow(offlineData)
                }
                if (synced) {
                    dao.updateStatus(rowId, "SYNCED")
                    Toast.makeText(
                        this@Laboratory_Form_2_Annex2G_Activity,
                        "Lab report submitted successfully.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    dao.updateStatus(rowId, "FAILED")
                    LabSyncWorker.schedule(this@Laboratory_Form_2_Annex2G_Activity)
                    Toast.makeText(
                        this@Laboratory_Form_2_Annex2G_Activity,
                        "Report saved. Will sync automatically when online.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                LabSyncWorker.schedule(this@Laboratory_Form_2_Annex2G_Activity)
                Toast.makeText(
                    this@Laboratory_Form_2_Annex2G_Activity,
                    "Report saved. Will sync automatically when online.",
                    Toast.LENGTH_LONG
                ).show()
            }

            startActivity(Intent(this@Laboratory_Form_2_Annex2G_Activity, Success_Activity::class.java))
            finish()
        }
    }

    // ── copies URI image to permanent app-internal storage ────────────────────
    private fun copyImageToPermanentStorage(uri: Uri): File? {
        return try {
            val mimeType  = contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("gif") -> "gif"
                else -> "jpg"
            }

            // save to files/lab_images/ — persists until app is uninstalled
            val dir = File(filesDir, "lab_images").also { it.mkdirs() }
            val destFile = File(dir, "lab_${System.currentTimeMillis()}.$extension")

            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("LAB_IMAGE", "Saved to: ${destFile.absolutePath}")
            destFile
        } catch (e: Exception) {
            Log.e("LAB_IMAGE", "Failed to copy image: ${e.message}", e)
            null
        }
    }

    // ── builds multipart and attempts upload — used for both immediate and retry
    fun tryUploadLabNow(data: LabFormOfflineData): Boolean {
        return try {
            val file = File(data.imagePath)
            if (!file.exists()) return false

            val imagePart = MultipartBody.Part.createFormData(
                "labResultImage",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )

            val response = ApiClient.getClient(this).submitLabReport(
                labName                      = data.labName.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabReceived              = data.dateLabReceived.toRequestBody("text/plain".toMediaTypeOrNull()),
                specimenCondition            = data.specimenCondition.toRequestBody("text/plain".toMediaTypeOrNull()),
                testTypesPerformed           = data.testTypesPerformed.toRequestBody("text/plain".toMediaTypeOrNull()),
                finalLabResult               = data.finalLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabSentDistrict          = data.dateLabSentDistrict.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateDistrictReceivedLabResult = data.dateDistrictReceivedLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                labResultImage               = imagePart
            ).execute()

            response.isSuccessful
        } catch (e: Exception) {
            Log.e("LAB_UPLOAD", "Upload failed: ${e.message}", e)
            false
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}