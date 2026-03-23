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
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.databinding.ActivityAnnex2Fimmediate3Binding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_3_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2Fimmediate3Binding
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate3Binding.inflate(layoutInflater)
        setContentView(binding.root)


        setupTravelHistorySpinner()


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
    }

    private fun setupTravelHistorySpinner() {
        val travelOptions = arrayOf("Yes", "No")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, travelOptions)
        binding.spinnerTravelHistory.setAdapter(adapter)

        binding.spinnerTravelHistory.setOnItemClickListener { _, _, position, _ ->
            val selectedTravel = travelOptions[position]

            when (selectedTravel) {
                "Yes" -> {
                    // Show destination field when "Yes" is selected
                    binding.tilDestination.visibility = View.VISIBLE
                    binding.etDestination.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.etDestination, InputMethodManager.SHOW_IMPLICIT)
                }
                "No" -> {
                    // Hide and clear destination field when "No" is selected
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
        datePickerDialog.show()
    }

    private fun validateImmediate3Form(): Boolean {
        var isValid = true


        if (binding.etDateOfOnset.text.isNullOrEmpty()) {
            binding.tilDateOfOnset.error = "Date of onset is required"
            isValid = false
        } else {
            binding.tilDateOfOnset.error = null
        }


        if (binding.spinnerTravelHistory.text.isNullOrEmpty()) {
            binding.tilTravelHistory.error = "Please select Travel history"
            isValid = false
        } else {
            binding.tilTravelHistory.error = null

            if (binding.spinnerTravelHistory.text.toString() == "Yes") {
                if (binding.etDestination.text.isNullOrEmpty()) {
                    binding.tilDestination.error = "Destination is required when travel history is 'Yes'"
                    isValid = false
                } else {
                    binding.tilDestination.error = null
                }
            }
        }

        // Validate Vaccine Doses (optional field with conditional validation)
        val dosesStr = binding.etVaccineDoses.text.toString().trim()
        if (dosesStr.isNotEmpty()) {
            val num = dosesStr.toIntOrNull()

            if (num == null) {
                binding.tilVaccineDoses.error = "Invalid number of doses"
                isValid = false
            } else if (num < 0) {
                binding.tilVaccineDoses.error = "Number of doses cannot be negative"
                isValid = false
            } else {
                binding.tilVaccineDoses.error = null

                if (num > 0 && binding.etDateLastVaccine.text.isNullOrEmpty()) {
                    binding.tilDateLastVaccine.error = "Date of last vaccination is required when doses > 0"
                    isValid = false
                } else {
                    binding.tilDateLastVaccine.error = null
                }
            }
        } else {
            binding.tilVaccineDoses.error = null
            binding.tilDateLastVaccine.error = null
        }


        if (binding.etDateSpecimen.text.isNullOrEmpty()) {
            binding.tilDateSpecimen.error = "Date specimen collected is required"
            isValid = false
        } else {
            binding.tilDateSpecimen.error = null
        }


        if (binding.etDateLab.text.isNullOrEmpty()) {
            binding.tilDateLab.error = "Date specimen sent to lab is required"
            isValid = false
        } else {
            binding.tilDateLab.error = null
        }


        if (binding.etLabResults.text.isNullOrEmpty()) {
            binding.tilLabResults.error = "Laboratory results are required"
            isValid = false
        } else {
            binding.tilLabResults.error = null
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
        }
        startActivity(intent)
        finish()
    }
}