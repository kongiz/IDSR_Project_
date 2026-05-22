package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.Annex2FData
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.databinding.ActivityAnnex2Fimmediate3Binding
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.applyWindowInsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_3_Activity : BaseActivity() {

    private lateinit var binding: ActivityAnnex2Fimmediate3Binding
    private val calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var isEditMode   = false
    private var editReportId = -1
    private var editData: Annex2FData? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets(
            topView    = binding.appBarLayout,
            bottomView = binding.btnNextAnnex3
        )

        // Read edit mode extras first
        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editReportId = intent.getIntExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, -1)
        editData     = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)

        setupTravelHistorySpinner()
        setupVaccineDosesListener()
        setupDatePickers()
        setupFieldListeners()

        if (isEditMode && editData != null) {
            prefillAnnex2F3(editData!!)
            binding.btnNextAnnex3.text = "Next (Editing)"
        }

        binding.btnBackAnnex3.setOnClickListener { finish() }
        binding.btnNextAnnex3.setOnClickListener {
            if (validateImmediate3Form()) passDataToNextScreen()
        }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Annex2F_Immediate_3_Activity")
    }


    private fun setupTravelHistorySpinner() {
        val options = arrayOf("Yes", "No")
        binding.spinnerTravelHistory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.spinnerTravelHistory.setOnItemClickListener { _, _, position, _ ->
            binding.tilTravelHistory.error = null
            when (options[position]) {
                "Yes" -> {
                    binding.tilDestination.visibility = View.VISIBLE
                    binding.etDestination.requestFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showSoftInput(binding.etDestination, InputMethodManager.SHOW_IMPLICIT)
                }
                "No"  -> {
                    binding.tilDestination.visibility = View.GONE
                    binding.etDestination.text?.clear()
                    binding.tilDestination.error = null
                }
            }
        }
    }


    private fun setupVaccineDosesListener() {
        binding.etVaccineDoses.addTextChangedListener {
            binding.tilVaccineDoses.error = null
            val num = it.toString().toIntOrNull() ?: 0
            if (num > 0) {
                binding.tilDateLastVaccine.visibility = View.VISIBLE
            } else {
                binding.tilDateLastVaccine.visibility = View.GONE
                binding.etDateLastVaccine.text?.clear()
                binding.tilDateLastVaccine.error = null
            }
        }
    }

    private fun setupDatePickers() {
        binding.etDateOfOnset.setOnClickListener {
            showDatePicker { binding.etDateOfOnset.setText(it); binding.tilDateOfOnset.error = null }
        }
        binding.etDateLastVaccine.setOnClickListener {
            showDatePicker { binding.etDateLastVaccine.setText(it); binding.tilDateLastVaccine.error = null }
        }
        binding.etDateSpecimen.setOnClickListener {
            showDatePicker { binding.etDateSpecimen.setText(it); binding.tilDateSpecimen.error = null }
        }
        binding.etDateLab.setOnClickListener {
            showDatePicker { binding.etDateLab.setText(it); binding.tilDateLab.error = null }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val cal = Calendar.getInstance().apply { set(year, month, day) }
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
        binding.etDestination.addTextChangedListener { binding.tilDestination.error = null }
        binding.etLabResults.addTextChangedListener  { binding.tilLabResults.error  = null }
    }



    private fun validateImmediate3Form(): Boolean {
        var isValid = true

        val onsetStr    = binding.etDateOfOnset.text.toString().trim()
        val specimenStr = binding.etDateSpecimen.text.toString().trim()
        val labStr      = binding.etDateLab.text.toString().trim()
        val travelHistory = binding.spinnerTravelHistory.text?.toString()?.trim() ?: ""
        val dosesStr    = binding.etVaccineDoses.text.toString().trim()
        val lastVaccineStr = binding.etDateLastVaccine.text.toString().trim()

        if (onsetStr.isEmpty()) {
            binding.tilDateOfOnset.error = "Required"; isValid = false
        } else binding.tilDateOfOnset.error = null

        if (specimenStr.isEmpty()) {
            binding.tilDateSpecimen.error = "Required"; isValid = false
        } else binding.tilDateSpecimen.error = null

        if (labStr.isEmpty()) {
            binding.tilDateLab.error = "Required"; isValid = false
        } else binding.tilDateLab.error = null

        if (binding.etLabResults.text.isNullOrEmpty()) {
            binding.tilLabResults.error = "Required"; isValid = false
        } else binding.tilLabResults.error = null

        if (travelHistory.isEmpty()) {
            binding.tilTravelHistory.error = "Required"; isValid = false
        } else {
            binding.tilTravelHistory.error = null
            if (travelHistory == "Yes" && binding.etDestination.text.isNullOrEmpty()) {
                binding.tilDestination.error = "Required when travel history is Yes"
                isValid = false
            } else {
                binding.tilDestination.error = null
            }
        }

        // Vaccine doses validation
        if (dosesStr.isNotEmpty()) {
            val num = dosesStr.toIntOrNull()
            if (num == null || num < 0) {
                binding.tilVaccineDoses.error = "Enter a valid number (0 or more)"
                isValid = false
            } else {
                binding.tilVaccineDoses.error = null
                if (num > 0 && lastVaccineStr.isEmpty()) {
                    binding.tilDateLastVaccine.error = "Required when doses > 0"
                    binding.tilDateLastVaccine.requestFocus()
                    isValid = false
                } else {
                    binding.tilDateLastVaccine.error = null
                }
            }
        }


        if (onsetStr.isNotEmpty() && specimenStr.isNotEmpty()) {
            try {
                val dateOnset    = sdf.parse(onsetStr)
                val dateSpecimen = sdf.parse(specimenStr)
                if (dateOnset != null && dateSpecimen != null && dateSpecimen.before(dateOnset)) {
                    binding.tilDateSpecimen.error = "Specimen cannot be collected before onset"
                    isValid = false
                }
            } catch (e: Exception) { /* no-op, already caught by empty checks */ }
        }


        if (specimenStr.isNotEmpty() && labStr.isNotEmpty()) {
            try {
                val dateSpecimen = sdf.parse(specimenStr)
                val dateLab      = sdf.parse(labStr)
                if (dateSpecimen != null && dateLab != null && dateLab.before(dateSpecimen)) {
                    binding.tilDateLab.error = "Sent to lab cannot be before collection"
                    isValid = false
                }
            } catch (e: Exception) { /* no-op */ }
        }

        return isValid
    }


    private fun prefillAnnex2F3(data: Annex2FData) {
        binding.etDateOfOnset.setText(data.dateOfOnset     ?: "")
        binding.spinnerTravelHistory.setText(data.travelHistory ?: "", false)

        if (data.travelHistory == "Yes") {
            binding.tilDestination.visibility = View.VISIBLE
            binding.etDestination.setText(data.destination ?: "")
        }

        binding.etVaccineDoses.setText(data.vaccineDoses   ?: "")

        val doses = data.vaccineDoses?.toIntOrNull() ?: 0
        if (doses > 0) {
            binding.tilDateLastVaccine.visibility = View.VISIBLE
            binding.etDateLastVaccine.setText(data.dateLastVaccine ?: "")
        } else {
            binding.tilDateLastVaccine.visibility = View.GONE
        }

        binding.etDateSpecimen.setText(data.dateSpecimen   ?: "")
        binding.etDateLab.setText(data.dateLab             ?: "")
        binding.etLabResults.setText(data.labResults       ?: "")
    }


    private fun passDataToNextScreen() {
        val received = intent.getParcelableExtra<immediateReportForm>("Annex2FReport")

        val annex2FReports = immediateReportForm(
            recordId             = received?.recordId             ?: "",
            country              = received?.country              ?: "",
            province             = received?.province             ?: "",
            district             = received?.district             ?: 0,
            site                 = received?.site                 ?: "",
            disease              = received?.disease              ?: "",
            inpatientOutpatient  = received?.inpatientOutpatient  ?: "",
            caseGeo              = received?.caseGeo              ?: "",
            dateSeen             = received?.dateSeen             ?: "",
            patientName          = received?.patientName          ?: "",
            dateOfBirth          = received?.dateOfBirth          ?: "",
            age                  = received?.age                  ?: 0,
            gender               = received?.gender               ?: "",
            address              = received?.address              ?: "",
            districtAnnex2       = received?.districtAnnex2       ?: "",
            urbanRural           = received?.urbanRural           ?: "",
            phoneNumber          = received?.phoneNumber          ?: "",
            occupation           = received?.occupation           ?: "",
            dateOfOnset          = binding.etDateOfOnset.text.toString().trim(),
            travelHistory        = binding.spinnerTravelHistory.text.toString().trim(),
            destination          = binding.etDestination.text.toString().trim(),
            vaccineDoses         = binding.etVaccineDoses.text.toString().trim(),
            dateLastVaccine      = binding.etDateLastVaccine.text.toString().trim(),
            dateSpecimen         = binding.etDateSpecimen.text.toString().trim(),
            dateLab              = binding.etDateLab.text.toString().trim(),
            labResults           = binding.etLabResults.text.toString().trim(),
            outcome              = received?.outcome              ?: "",
            classification       = received?.classification       ?: "",
            dateFacilityNotified = received?.dateFacilityNotified ?: "",
            dateSentDistrict     = received?.dateSentDistrict     ?: "",
            reporterName         = received?.reporterName         ?: ""
        )

        startActivity(
            Intent(this, Annex2F_Immediate_4_Activity::class.java).apply {
                putExtra("Annex2FReport", annex2FReports)
                putExtra(EditModeExtras.EXTRA_EDIT_MODE,      isEditMode)
                putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, editReportId)
                putExtra(EditModeExtras.EXTRA_EDIT_DATA,      editData)
            }
        )
    }
}