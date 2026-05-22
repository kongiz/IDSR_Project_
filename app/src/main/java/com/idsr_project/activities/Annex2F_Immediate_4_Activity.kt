package com.idsr_project.activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.idsr_project.Model.Annex2FData
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.data.repository.OfflineRepository
import com.idsr_project.data.repository.SubmitResult
import com.idsr_project.databinding.ActivityAnnex2Fimmediate4Binding
import com.idsr_project.sync.SyncWorker
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_4_Activity : BaseActivity() {

    private lateinit var binding: ActivityAnnex2Fimmediate4Binding
    private val calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val repository by lazy { OfflineRepository(this) }

    private var isEditMode   = false
    private var editReportId = -1
    private var editData: Annex2FData? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets(
            topView    = binding.appBarLayout,
            bottomView = binding.btnNextAnnex4
        )



        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editReportId = intent.getIntExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, -1)
        editData     = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)

        setupOutcomeSpinner()
        setupClassificationSpinner()
        setupDatePickers()
        setupFieldListeners()
        autoFillReporterName()

        if (isEditMode && editData != null) {
            prefillAnnex2F4(editData!!)
            binding.btnNextAnnex4.text = "Update Report"
        }

        binding.btnBackAnnex4.setOnClickListener { finish() }
        binding.btnNextAnnex4.setOnClickListener {
            if (validateImmediate4Form()) submitAnnex2FImmediateToDB()
        }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Annex2F_Immediate_4_Activity")
    }

    private fun setupOutcomeSpinner() {
        binding.spinnerOutcome.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
                resources.getStringArray(R.array.Outcome))
        )
        binding.spinnerOutcome.setOnItemClickListener { _, _, _, _ ->
            binding.tilOutcome.error = null
        }
    }

    private fun setupClassificationSpinner() {
        binding.spinnerClassification.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
                resources.getStringArray(R.array.Classification))
        )
        binding.spinnerClassification.setOnItemClickListener { _, _, _, _ ->
            binding.tilClassification.error = null
        }
    }


    private fun setupDatePickers() {
        binding.etDateFacilityNotified.setOnClickListener {
            showDatePicker { binding.etDateFacilityNotified.setText(it); binding.tilDateFacilityNotified.error = null }
        }
        binding.etDateSentDistrict.setOnClickListener {
            showDatePicker { binding.etDateSentDistrict.setText(it); binding.tilDateSentDistrict.error = null }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val dialog = DatePickerDialog(
            this,
            { _, y, m, d ->
                val cal = Calendar.getInstance().apply { set(y, m, d) }
                onDateSelected(sdf.format(cal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }


    private fun setupFieldListeners() {
        binding.etReporterName.addTextChangedListener { binding.tilReporterName.error = null }
    }

    @SuppressLint("SetTextI18n")
    private fun autoFillReporterName() {
        val name = SessionManager.getFullName(this) ?: ""
        val role = SessionManager.getUserRole(this) ?: ""

        if (!isEditMode) {
            binding.etReporterName.setText("$name - $role")
        }
    }



    private fun validateImmediate4Form(): Boolean {
        var isValid = true

        val outcome         = binding.spinnerOutcome.text.toString().trim()
        val classification  = binding.spinnerClassification.text.toString().trim()
        val notifiedDateStr = binding.etDateFacilityNotified.text.toString().trim()
        val sentDistrictStr = binding.etDateSentDistrict.text.toString().trim()
        val reporter        = binding.etReporterName.text.toString().trim()

        if (outcome.isEmpty()) {
            binding.tilOutcome.error = "Required"; isValid = false
        } else binding.tilOutcome.error = null

        if (classification.isEmpty()) {
            binding.tilClassification.error = "Required"; isValid = false
        } else binding.tilClassification.error = null

        if (notifiedDateStr.isEmpty()) {
            binding.tilDateFacilityNotified.error = "Required"; isValid = false
        } else binding.tilDateFacilityNotified.error = null

        if (sentDistrictStr.isEmpty()) {
            binding.tilDateSentDistrict.error = "Required"; isValid = false
        } else binding.tilDateSentDistrict.error = null

        if (reporter.isEmpty()) {
            binding.tilReporterName.error = "Required"; isValid = false
        } else binding.tilReporterName.error = null


        if (notifiedDateStr.isNotEmpty() && sentDistrictStr.isNotEmpty()) {
            try {
                val dateNotified = sdf.parse(notifiedDateStr)
                val dateSent     = sdf.parse(sentDistrictStr)
                if (dateNotified != null && dateSent != null && dateSent.before(dateNotified)) {
                    binding.tilDateSentDistrict.error = "Cannot be before notification date"
                    isValid = false
                }
            } catch (e: Exception) { /* no-op */ }
        }

        return isValid
    }


    private fun submitAnnex2FImmediateToDB() {
        binding.btnNextAnnex4.isEnabled = false
        binding.btnNextAnnex4.text      = if (isEditMode) "Updating..." else "Saving..."

        val received = intent.getParcelableExtra<immediateReportForm>("Annex2FReport")
        if (received == null) {
            Toast.makeText(this, "No report data received", Toast.LENGTH_SHORT).show()
            binding.btnNextAnnex4.isEnabled = true
            binding.btnNextAnnex4.text      = if (isEditMode) "Update Report" else "Submit Form"
            return
        }

        val finalForm = received.copy(
            outcome              = binding.spinnerOutcome.text.toString().trim(),
            classification       = binding.spinnerClassification.text.toString().trim(),
            dateFacilityNotified = binding.etDateFacilityNotified.text.toString().trim(),
            dateSentDistrict     = binding.etDateSentDistrict.text.toString().trim(),
            reporterName         = binding.etReporterName.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                if (isEditMode && editReportId != -1) {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.getClient(this@Annex2F_Immediate_4_Activity)
                            .editImmediateReport(editReportId, finalForm)
                            .execute()
                    }
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@Annex2F_Immediate_4_Activity,
                            "Report updated successfully", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@Annex2F_Immediate_4_Activity, Success_Activity::class.java))
                        finish()
                    } else {
                        val errorMsg = when (response.code()) {
                            403  -> "Edit window expired or no permission"
                            404  -> "Report not found"
                            else -> "Update failed. Please try again."
                        }
                        Toast.makeText(this@Annex2F_Immediate_4_Activity, errorMsg, Toast.LENGTH_LONG).show()
                        binding.btnNextAnnex4.isEnabled = true
                        binding.btnNextAnnex4.text      = "Update Report"
                    }
                } else {
                    val json = Gson().toJson(finalForm)
                    when (val result = repository.submitReport("ANNEX2F", json)) {
                        is SubmitResult.SyncedOnline -> {
                            Toast.makeText(this@Annex2F_Immediate_4_Activity,
                                result.message, Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@Annex2F_Immediate_4_Activity, Success_Activity::class.java))
                            finish()
                        }
                        is SubmitResult.SavedOffline -> {
                            Toast.makeText(this@Annex2F_Immediate_4_Activity,
                                "Saved offline. Will sync when online.", Toast.LENGTH_LONG).show()
                            SyncWorker.schedule(this@Annex2F_Immediate_4_Activity)
                            startActivity(Intent(this@Annex2F_Immediate_4_Activity, Success_Activity::class.java))
                            finish()
                        }
                        is SubmitResult.Error -> {
                            Toast.makeText(this@Annex2F_Immediate_4_Activity,
                                "Error: ${result.message}", Toast.LENGTH_LONG).show()
                            binding.btnNextAnnex4.isEnabled = true
                            binding.btnNextAnnex4.text      = "Submit Form"
                        }
                    }
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                binding.btnNextAnnex4.isEnabled = true
                binding.btnNextAnnex4.text      = if (isEditMode) "Update Report" else "Submit Form"
                Toast.makeText(this@Annex2F_Immediate_4_Activity,
                    "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun prefillAnnex2F4(data: Annex2FData) {
        binding.spinnerOutcome.setText(data.outcome               ?: "", false)
        binding.spinnerClassification.setText(data.classification ?: "", false)
        binding.etDateFacilityNotified.setText(data.dateFacilityNotified ?: "")
        binding.etDateSentDistrict.setText(data.dateSentDistrict  ?: "")
        binding.etReporterName.setText(data.reporterName          ?: "")
    }
}