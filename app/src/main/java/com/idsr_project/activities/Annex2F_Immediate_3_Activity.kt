package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.Annex2FData
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.databinding.ActivityAnnex2Fimmediate3Binding
import com.idsr_project.utils.EditModeExtras
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_3_Activity : BaseActivity() {
    private lateinit var binding: ActivityAnnex2Fimmediate3Binding
    private val calendar = Calendar.getInstance()

    private var isEditMode   = false
    private var editReportId = -1
    private var editData: Annex2FData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate3Binding.inflate(layoutInflater)
        setContentView(binding.root)


        setupTravelHistorySpinner()

        binding.etVaccineDoses.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val num = s.toString().toIntOrNull() ?: 0
                if (num > 0) {
                    binding.tilDateLastVaccine.visibility = View.VISIBLE
                } else {
                    binding.tilDateLastVaccine.visibility = View.GONE
                    binding.etDateLastVaccine.text?.clear()
                    binding.tilDateLastVaccine.error = null
                }
            }
        })


        binding.btnBackAnnex3.setOnClickListener {
            finish()
        }


        binding.etDateOfOnset.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateOfOnset.setText(dateString)
            }
        }

        binding.etDateLastVaccine.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateLastVaccine.setText(dateString)
            }
        }

        binding.etDateSpecimen.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateSpecimen.setText(dateString)
            }
        }

        binding.etDateLab.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateLab.setText(dateString)
            }
        }


        binding.btnNextAnnex3.setOnClickListener {
            if (validateImmediate3Form()) {
                passDataToNextScreen()
            }
        }
        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editReportId = intent.getIntExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, -1)
        editData     = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)

        if (isEditMode && editData != null) {
            prefillAnnex2F3(editData!!)
            binding.btnNextAnnex3.text = "Next (Editing)"
        }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Annex2F_Immediate_3_Activity")
    }

    private fun setupTravelHistorySpinner() {
        val travelOptions = arrayOf("Yes", "No")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, travelOptions)
        binding.spinnerTravelHistory.setAdapter(adapter)

        binding.spinnerTravelHistory.setOnItemClickListener { _, _, position, _ ->
            val selectedTravel = travelOptions[position]

            when (selectedTravel) {
                "Yes" -> {
                    binding.tilDestination.visibility = View.VISIBLE
                    binding.etDestination.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.etDestination, InputMethodManager.SHOW_IMPLICIT)
                }
                "No" -> {
                    binding.tilDestination.visibility = View.GONE
                    binding.etDestination.text?.clear()
                    binding.tilDestination.error = null
                }
            }

            Toast.makeText(this, "Travel history: $selectedTravel", Toast.LENGTH_SHORT).show()
        }
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

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun validateImmediate3Form(): Boolean {
        var isValid = true
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val onsetStr = binding.etDateOfOnset.text.toString().trim()
        val travelHistory = binding.spinnerTravelHistory.text?.toString()?.trim() ?: ""
        val destination = binding.etDestination.text.toString().trim()
        val dosesStr = binding.etVaccineDoses.text.toString().trim()
        val lastVaccineStr = binding.etDateLastVaccine.text.toString().trim()
        val specimenStr = binding.etDateSpecimen.text.toString().trim()
        val labStr = binding.etDateLab.text.toString().trim()
        val labResults = binding.etLabResults.text.toString().trim()

        if (onsetStr.isEmpty()) {
            binding.tilDateOfOnset.error = "Date of onset is required"
            isValid = false
        } else binding.tilDateOfOnset.error = null

        if (specimenStr.isEmpty()) {
            binding.tilDateSpecimen.error = "Date specimen collected is required"
            isValid = false
        } else binding.tilDateSpecimen.error = null

        if (labStr.isEmpty()) {
            binding.tilDateLab.error = "Date specimen sent to lab is required"
            isValid = false
        } else binding.tilDateLab.error = null

        if (labResults.isEmpty()) {
            binding.tilLabResults.error = "Laboratory results are required"
            isValid = false
        } else binding.tilLabResults.error = null


        if (travelHistory.isEmpty()) {
            binding.tilTravelHistory.error = "Please select Travel history"
            isValid = false
        } else {
            binding.tilTravelHistory.error = null
            if (travelHistory == "Yes" && destination.isEmpty()) {
                binding.tilDestination.error = "Destination is required when travel history is 'Yes'"
                isValid = false
            } else {
                binding.tilDestination.error = null
            }
        }

        if (dosesStr.isNotEmpty()) {
            val num = dosesStr.toIntOrNull()
            if (num == null || num < 0) {
                binding.tilVaccineDoses.error = "Enter a valid number (0 or more)"
                isValid = false
            } else {
                binding.tilVaccineDoses.error = null
                if (num > 0 && lastVaccineStr.isEmpty()) {
                    binding.tilDateLastVaccine.error = "Required when doses > 0"
                    isValid = false

                    binding.tilDateLastVaccine.requestFocus()
                } else {
                    binding.tilDateLastVaccine.error = null
                }
            }
        } else {
            binding.tilVaccineDoses.error = null
            binding.tilDateLastVaccine.error = null
        }

        if (onsetStr.isNotEmpty() && specimenStr.isNotEmpty()) {
            try {
                val dateOnset = sdf.parse(onsetStr)
                val dateSpecimen = sdf.parse(specimenStr)
                if (dateSpecimen != null && dateOnset != null && dateSpecimen.before(dateOnset)) {
                    binding.tilDateSpecimen.error = "Specimen cannot be collected before onset"
                    isValid = false
                }
            } catch (e: Exception) { /* Handled by initial empty checks */ }
        }

        if (specimenStr.isNotEmpty() && labStr.isNotEmpty()) {
            try {
                val dateSpecimen = sdf.parse(specimenStr)
                val dateLab = sdf.parse(labStr)
                if (dateLab != null && dateSpecimen != null && dateLab.before(dateSpecimen)) {
                    binding.tilDateLab.error = "Sent to lab cannot be before collection"
                    isValid = false
                }
            } catch (e: Exception) { /* Handled by initial empty checks */ }
        }
        return isValid
    }

    private fun passDataToNextScreen() {
        val dateOfOnset = binding.etDateOfOnset.text.toString().trim()
        val travelHistory = binding.spinnerTravelHistory.text.toString().trim()
        val destination = binding.etDestination.text.toString().trim()
        val vaccineDoses = binding.etVaccineDoses.text.toString().trim()
        val dateLastVaccine = binding.etDateLastVaccine.text.toString().trim()
        val dateSpecimen = binding.etDateSpecimen.text.toString().trim()
        val dateLab = binding.etDateLab.text.toString().trim()
        val labResults = binding.etLabResults.text.toString().trim()

        val receivedAnnex2FReport = intent.getParcelableExtra<immediateReportForm>("Annex2FReport")

        val annex2FReports = immediateReportForm(
            recordId = receivedAnnex2FReport?.recordId ?: "",
            country = receivedAnnex2FReport?.country ?: "",
            province = receivedAnnex2FReport?.province ?: "",
            district = receivedAnnex2FReport?.district ?: 0,
            site = receivedAnnex2FReport?.site ?: "",
            disease = receivedAnnex2FReport?.disease ?: "",
            inpatientOutpatient = receivedAnnex2FReport?.inpatientOutpatient ?: "",
            caseGeo = receivedAnnex2FReport?.caseGeo ?: "",
            dateSeen = receivedAnnex2FReport?.dateSeen ?: "",
            patientName = receivedAnnex2FReport?.patientName ?: "",
            dateOfBirth = receivedAnnex2FReport?.dateOfBirth ?: "",
            age = receivedAnnex2FReport?.age ?: 0,
            gender = receivedAnnex2FReport?.gender ?: "",
            address = receivedAnnex2FReport?.address ?: "",
            districtAnnex2 = receivedAnnex2FReport?.districtAnnex2 ?: "",
            urbanRural = receivedAnnex2FReport?.urbanRural ?: "",
            phoneNumber = receivedAnnex2FReport?.phoneNumber ?: "",
            occupation = receivedAnnex2FReport?.occupation ?: "",
            dateOfOnset = dateOfOnset,
            travelHistory = travelHistory,
            destination = destination,
            vaccineDoses = vaccineDoses,
            dateLastVaccine = dateLastVaccine,
            dateSpecimen = dateSpecimen,
            dateLab = dateLab,
            labResults = labResults,
            outcome = receivedAnnex2FReport?.outcome ?: "",
            classification = receivedAnnex2FReport?.classification ?: "",
            dateFacilityNotified = receivedAnnex2FReport?.dateFacilityNotified ?: "",
            dateSentDistrict = receivedAnnex2FReport?.dateSentDistrict ?: "",
            reporterName = receivedAnnex2FReport?.reporterName ?: ""
        )

        val intent = Intent(this, Annex2F_Immediate_4_Activity::class.java).apply {
            putExtra("Annex2FReport", annex2FReports)
            putExtra(EditModeExtras.EXTRA_EDIT_MODE, isEditMode)
            putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, editReportId)
            putExtra(EditModeExtras.EXTRA_EDIT_DATA, editData)
        }
        startActivity(intent)
    }

    private fun prefillAnnex2F3(data: Annex2FData) {
        binding.etDateOfOnset.setText(data.dateOfOnset ?: "")
        binding.spinnerTravelHistory.setText(data.travelHistory ?: "", false)

        if (data.travelHistory == "Yes") {
            binding.tilDestination.visibility = View.VISIBLE
            binding.etDestination.setText(data.destination ?: "")
        }

        binding.etVaccineDoses.setText(data.vaccineDoses ?: "")


        val doses = data.vaccineDoses?.toIntOrNull() ?: 0
        if (doses > 0) {
            binding.tilDateLastVaccine.visibility = View.VISIBLE
            binding.etDateLastVaccine.setText(data.dateLastVaccine ?: "")
        } else {
            binding.tilDateLastVaccine.visibility = View.GONE
        }

        binding.etDateSpecimen.setText(data.dateSpecimen ?: "")
        binding.etDateLab.setText(data.dateLab ?: "")
        binding.etLabResults.setText(data.labResults ?: "")
    }
}