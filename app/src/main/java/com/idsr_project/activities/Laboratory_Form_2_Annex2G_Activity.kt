package com.idsr_project.activities

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.idsr_project.Model.LabReportData
import com.idsr_project.api.ApiClient
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.data.local.PendingReportEntity
import com.idsr_project.data.repository.OfflineRepository
import com.idsr_project.databinding.ActivityLaboratoryForm2Annex2GactivityBinding
import com.idsr_project.sync.LabSyncWorker
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Laboratory_Form_2_Annex2G_Activity : BaseActivity() {

    private lateinit var binding: ActivityLaboratoryForm2Annex2GactivityBinding
    private val calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val photoUris = mutableListOf<Uri>()
    private var tempCameraUri: Uri? = null
    private val repository by lazy { OfflineRepository(this) }

    data class LabFormOfflineData(
        val labName: String,
        val dateLabReceived: String,
        val specimenCondition: String,
        val testTypesPerformed: String,
        val finalLabResult: String,
        val dateLabSentDistrict: String,
        val dateDistrictReceivedLabResult: String,
        val imagePaths: List<String> // Changed to plural
    )

    private var galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            photoUris.addAll(it)
            updateImagePreview()
        }
    }

    private var cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            photoUris.add(tempCameraUri!!)
            updateImagePreview()
        }
    }

    private var isEditMode   = false
    private var editReportId = -1
    private var editData: LabReportData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLaboratoryForm2Annex2GactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(
            topView    = binding.appBarLayout,
            bottomView = binding.btnLabTech
        )

        setupDropdowns()
        setupFieldListeners()
        setupDatePickers()
        handleBackPress()

        binding.btnUploadLabResult.setOnClickListener { showImagePickerDialog() }
        binding.btnBackAnnex2G2.setOnClickListener { showExitWarning() }

        binding.btnLabTech.setOnClickListener {
            if (validateLabFormForLabTech2()) {
                submitLabForm2()
            }
        }
        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editReportId = intent.getIntExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, -1)
        editData     = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)

        if (isEditMode && editData != null) {
            prefillLabForm2(editData!!)
            binding.btnLabTech.text = if (isEditMode) "Update Form" else "Submit Form"

        }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Laboratory_Form_2_Annex2G_Activity")
    }



    private fun updateImagePreview() {
        if (photoUris.isNotEmpty()) {
            binding.cardImagePreview.visibility = View.VISIBLE
            binding.imgLabResultPreview.setImageURI(photoUris.last())
            binding.tvFinalLabResult.text = "Final Laboratory Result images (${photoUris.size} attached)"
        }
    }

    private fun setupDropdowns() {
        val specimenConditions = arrayOf("Adequate", "Not Adequate")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, specimenConditions)
        binding.spinnerSpecimenCon.setAdapter(adapter)
    }

    private fun setupDatePickers() {
        binding.etDateLabReceived.setOnClickListener {
            showDatePicker(null) { binding.etDateLabReceived.setText(it) }
        }

        binding.etDateLabSentDistrict.setOnClickListener {
            val minDate = try { sdf.parse(binding.etDateLabReceived.text.toString())?.time } catch (e: Exception) { null }
            showDatePicker(minDate) { binding.etDateLabSentDistrict.setText(it) }
        }

        binding.etDateDistrictReceivedLabResult.setOnClickListener {
            val minDate = try { sdf.parse(binding.etDateLabSentDistrict.text.toString())?.time } catch (e: Exception) { null }
            showDatePicker(minDate) { binding.etDateDistrictReceivedLabResult.setText(it) }
        }
    }

    private fun showDatePicker(minDate: Long?, onDateSelected: (String) -> Unit) {
        val dialog = DatePickerDialog(this, { _, y, m, d ->
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            onDateSelected(sdf.format(cal.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        dialog.datePicker.maxDate = System.currentTimeMillis()
        minDate?.let { dialog.datePicker.minDate = it }
        dialog.show()
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Upload Image")
            .setItems(arrayOf("Gallery (Select Multiple)", "Camera")) { _, which ->
                when (which) {
                    0 -> galleryLauncher.launch("image/*")
                    1 -> checkCameraPermission()
                }
            }.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Lab Result ${System.currentTimeMillis()}")
        }
        tempCameraUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        tempCameraUri?.let { cameraLauncher.launch(it) }
    }

    private fun validateLabFormForLabTech2(): Boolean {
        var isValid = true
        val fields = listOf(
            binding.etLabName to binding.tilLabName,
            binding.etDateLabReceived to binding.tilDateLabReceived,
            binding.etTestTypesPerformed to binding.tilTestTypesPerformed,
            binding.etFinalLAbResult to binding.tilFinalLabResult,
            binding.etDateLabSentDistrict to binding.tilDateLabSentDistrict,
            binding.etDateDistrictReceivedLabResult to binding.tilDateDistrictReceivedLabResult
        )

        for ((et, til) in fields) {
            if (et.text.isNullOrEmpty()) {
                til.error = "Required"
                isValid = false
            }
        }

        if (binding.spinnerSpecimenCon.text.isNullOrEmpty()) {
            binding.tilSpecimenCon.error = "Required"
            isValid = false
        }

        if (photoUris.isEmpty() && !isEditMode) {
            Toast.makeText(this, "Please attach at least one lab result image", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun submitLabForm2() {
        binding.btnLabTech.isEnabled = false
        binding.btnLabTech.text      = if (isEditMode) "Updating..." else "Saving..."

        lifecycleScope.launch {
            try {
                if (isEditMode && editReportId != -1) {
                    val savedPaths = if (photoUris.isNotEmpty()) {
                        withContext(Dispatchers.IO) { photoUris.mapNotNull { copyAndCompressImage(it) } }
                    } else emptyList()

                    val response = withContext(Dispatchers.IO) {
                        val imageParts = savedPaths.map { path ->
                            val file        = File(path)
                            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("labResultImage", file.name, requestFile)
                        }

                        ApiClient.getClient(this@Laboratory_Form_2_Annex2G_Activity)
                            .editLabReport(
                                id                             = editReportId,
                                labName                        = binding.etLabName.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                                dateLabReceived                = binding.etDateLabReceived.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                                specimenCondition              = binding.spinnerSpecimenCon.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                                testTypesPerformed             = binding.etTestTypesPerformed.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                                finalLabResult                 = binding.etFinalLAbResult.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                                dateLabSentDistrict            = binding.etDateLabSentDistrict.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                                dateDistrictReceivedLabResult  = binding.etDateDistrictReceivedLabResult.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                                labResultImages                = imageParts
                            ).execute()
                    }

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@Laboratory_Form_2_Annex2G_Activity,
                            "Lab report updated successfully", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@Laboratory_Form_2_Annex2G_Activity, Success_Activity::class.java))
                        finish()
                    } else {
                        val errorMsg = when (response.code()) {
                            403  -> "Edit window has expired or you don't have permission"
                            404  -> "Report not found"
                            else -> "Update failed. Please try again."
                        }
                        Toast.makeText(this@Laboratory_Form_2_Annex2G_Activity, errorMsg, Toast.LENGTH_LONG).show()
                        binding.btnLabTech.isEnabled = true
                        binding.btnLabTech.text      = "Update Report"
                    }
                } else {
                    val savedPaths = withContext(Dispatchers.IO) {
                        photoUris.mapNotNull { copyAndCompressImage(it) }
                    }

                    val offlineData = LabFormOfflineData(
                        labName                        = binding.etLabName.text.toString().trim(),
                        dateLabReceived                = binding.etDateLabReceived.text.toString().trim(),
                        specimenCondition              = binding.spinnerSpecimenCon.text.toString().trim(),
                        testTypesPerformed             = binding.etTestTypesPerformed.text.toString().trim(),
                        finalLabResult                 = binding.etFinalLAbResult.text.toString().trim(),
                        dateLabSentDistrict            = binding.etDateLabSentDistrict.text.toString().trim(),
                        dateDistrictReceivedLabResult  = binding.etDateDistrictReceivedLabResult.text.toString().trim(),
                        imagePaths                     = savedPaths
                    )

                    val json  = Gson().toJson(offlineData)
                    val dao   = AppDatabase.getInstance(this@Laboratory_Form_2_Annex2G_Activity).pendingReportDao()
                    val rowId = dao.insert(PendingReportEntity(
                        formType    = "LAB",
                        reportJson  = json,
                        submittedBy = SessionManager.getFullName(this@Laboratory_Form_2_Annex2G_Activity) ?: "Unknown"
                    ))

                    if (repository.isOnline()) {
                        val synced = withContext(Dispatchers.IO) { tryUploadLabNow(offlineData) }
                        if (synced) dao.updateStatus(rowId, "SYNCED")
                        else LabSyncWorker.schedule(this@Laboratory_Form_2_Annex2G_Activity)
                    } else {
                        LabSyncWorker.schedule(this@Laboratory_Form_2_Annex2G_Activity)
                    }

                    startActivity(Intent(this@Laboratory_Form_2_Annex2G_Activity, Success_Activity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                binding.btnLabTech.isEnabled = true
                binding.btnLabTech.text      = if (isEditMode) "Update Report" else "Submit Form"
                Toast.makeText(this@Laboratory_Form_2_Annex2G_Activity,
                    "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyAndCompressImage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val dir = File(filesDir, "lab_images").also { it.mkdirs() }
            val file = File(dir, "lab_${UUID.randomUUID()}.jpg")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    private fun tryUploadLabNow(data: LabFormOfflineData): Boolean {
        return try {
            val imageParts = data.imagePaths.map { path ->
                val file = File(path)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("labResultImage", file.name, requestFile)
            }

            val response = ApiClient.getClient(this).submitLabReport(
                labName = data.labName.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabReceived = data.dateLabReceived.toRequestBody("text/plain".toMediaTypeOrNull()),
                specimenCondition = data.specimenCondition.toRequestBody("text/plain".toMediaTypeOrNull()),
                testTypesPerformed = data.testTypesPerformed.toRequestBody("text/plain".toMediaTypeOrNull()),
                finalLabResult = data.finalLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateLabSentDistrict = data.dateLabSentDistrict.toRequestBody("text/plain".toMediaTypeOrNull()),
                dateDistrictReceivedLabResult = data.dateDistrictReceivedLabResult.toRequestBody("text/plain".toMediaTypeOrNull()),
                labResultImages = imageParts
            ).execute()

            response.isSuccessful
        } catch (e: Exception) { false }
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { showExitWarning() }
        })
    }

    private fun showExitWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Results?")
            .setMessage("All captured images and data will be lost.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupFieldListeners() {
        listOf(
            binding.etLabName               to binding.tilLabName,
            binding.etDateLabReceived       to binding.tilDateLabReceived,
            binding.etTestTypesPerformed    to binding.tilTestTypesPerformed,
            binding.etFinalLAbResult        to binding.tilFinalLabResult,
            binding.etDateLabSentDistrict   to binding.tilDateLabSentDistrict,
            binding.etDateDistrictReceivedLabResult to binding.tilDateDistrictReceivedLabResult
        ).forEach { (et, til) ->
            et.addTextChangedListener { til.error = null }
        }
        binding.spinnerSpecimenCon.addTextChangedListener { binding.tilSpecimenCon.error = null }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
    private fun prefillLabForm2(data: LabReportData) {
        binding.etLabName.setText(data.lab_name ?: "")
        binding.etDateLabReceived.setText(data.date_lab_received ?: "")
        binding.spinnerSpecimenCon.setText(data.specimen_condition ?: "", false)
        binding.etTestTypesPerformed.setText(data.test_types_performed ?: "")
        binding.etFinalLAbResult.setText(data.final_lab_result ?: "")
        binding.etDateLabSentDistrict.setText(data.date_lab_sent_district ?: "")
        binding.etDateDistrictReceivedLabResult.setText(data.date_district_received_lab_result ?: "")
    }
}