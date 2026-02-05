package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityAnnex2Fimmediate4Binding
import com.idsr_project.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_4_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2Fimmediate4Binding
    private val calendar = Calendar.getInstance()
    private var isSubmitting = false // Flag to prevent multiple submissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate4Binding.inflate(layoutInflater)
        setContentView(binding.root)


        setupOutcomeSpinner()


        setupClassificationSpinner()


        binding.btnBackAnnex4.setOnClickListener {
            finish()
        }


        binding.etDateFacilityNotified.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateFacilityNotified.setText(dateString)
            }
        }

        binding.etDateSentDistrict.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateSentDistrict.setText(dateString)
            }
        }


        binding.btnNextAnnex4.setOnClickListener {
            if (!isSubmitting) {
                if (validateImmediate4Form()) {
                    submitAnnex2FImmediateToDB()
                }
            } else {
                Toast.makeText(this, "Form is being submitted, please wait...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupOutcomeSpinner() {
        // Get outcome options from resources
        val outcomeOptions = resources.getStringArray(R.array.Outcome)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, outcomeOptions)
        binding.spinnerOutcome.setAdapter(adapter)

        binding.spinnerOutcome.setOnItemClickListener { _, _, position, _ ->
            val selectedOutcome = outcomeOptions[position]
            Toast.makeText(this, "Selected Outcome: $selectedOutcome", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClassificationSpinner() {
        // Get classification options from resources
        val classificationOptions = resources.getStringArray(R.array.Classification)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, classificationOptions)
        binding.spinnerClassification.setAdapter(adapter)

        binding.spinnerClassification.setOnItemClickListener { _, _, position, _ ->
            val selectedClassification = classificationOptions[position]
            Toast.makeText(this, "Selected Classification: $selectedClassification", Toast.LENGTH_SHORT).show()
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

    private fun validateImmediate4Form(): Boolean {
        var isValid = true

        // Validate Outcome
        if (binding.spinnerOutcome.text.isNullOrEmpty() ||
            binding.spinnerOutcome.text.toString() == "Select an outcome") {
            binding.tilOutcome.error = "Please select an outcome"
            isValid = false
        } else {
            binding.tilOutcome.error = null
        }

        // Validate Classification
        if (binding.spinnerClassification.text.isNullOrEmpty() ||
            binding.spinnerClassification.text.toString() == "Select a classification") {
            binding.tilClassification.error = "Please select a classification"
            isValid = false
        } else {
            binding.tilClassification.error = null
        }

        // Validate Date Facility Notified
        if (binding.etDateFacilityNotified.text.isNullOrEmpty()) {
            binding.tilDateFacilityNotified.error = "Date facility notified is required"
            isValid = false
        } else {
            binding.tilDateFacilityNotified.error = null
        }

        // Validate Date Sent to District
        if (binding.etDateSentDistrict.text.isNullOrEmpty()) {
            binding.tilDateSentDistrict.error = "Date form sent to district is required"
            isValid = false
        } else {
            binding.tilDateSentDistrict.error = null
        }

        // Validate Reporter Name
        if (binding.etReporterName.text.isNullOrEmpty()) {
            binding.tilReporterName.error = "Reporter name is required"
            isValid = false
        } else {
            binding.tilReporterName.error = null
        }

        return isValid
    }

    private fun submitAnnex2FImmediateToDB() {

        if (isSubmitting) {
            return
        }


        isSubmitting = true
        binding.btnNextAnnex4.isEnabled = false
        binding.btnNextAnnex4.text = "Submitting..."

        val outcome = binding.spinnerOutcome.text.toString().trim()
        val classification = binding.spinnerClassification.text.toString().trim()
        val dateFacilityNotified = binding.etDateFacilityNotified.text.toString().trim()
        val dateSentDistrict = binding.etDateSentDistrict.text.toString().trim()
        val reporterName = binding.etReporterName.text.toString().trim()

        val receivedAnnex2Report = intent.getParcelableExtra<immediateReportForm>("Annex2FReport")
        if (receivedAnnex2Report == null) {
            Toast.makeText(this, "No report data received", Toast.LENGTH_SHORT).show()
            isSubmitting = false
            binding.btnNextAnnex4.isEnabled = true
            binding.btnNextAnnex4.text = "Submit Form"
            return
        }

        val finalForm = receivedAnnex2Report.copy(
            outcome = outcome,
            classification = classification,
            dateFacilityNotified = dateFacilityNotified,
            dateSentDistrict = dateSentDistrict,
            reporterName = reporterName
        )

        ApiClient.getClient(context = this).submitImmediateReportData(finalForm)
            .enqueue(object : retrofit2.Callback<ResponseApi> {
                override fun onResponse(
                    call: retrofit2.Call<ResponseApi>,
                    response: retrofit2.Response<ResponseApi>
                ) {
                    // Reset submitting flag
                    isSubmitting = false
                    binding.btnNextAnnex4.isEnabled = true
                    binding.btnNextAnnex4.text = "Submit Form"

                    if (response.isSuccessful && response.body() != null) {
                        val message = response.body()!!.msg
                        Toast.makeText(
                            this@Annex2F_Immediate_4_Activity,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(
                            this@Annex2F_Immediate_4_Activity,
                            Success_Activity::class.java
                        )
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@Annex2F_Immediate_4_Activity,
                            "Failed to submit form",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<ResponseApi>, t: Throwable) {
                    // Reset submitting flag on failure
                    isSubmitting = false
                    binding.btnNextAnnex4.isEnabled = true
                    binding.btnNextAnnex4.text = "Submit Form"

                    Toast.makeText(
                        this@Annex2F_Immediate_4_Activity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}