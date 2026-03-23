package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.idsr_project.R
import com.idsr_project.Model.reportFormToLabWithSpecimen
import com.idsr_project.data.repository.OfflineRepository
import com.idsr_project.data.repository.SubmitResult
import com.idsr_project.databinding.ActivityLaboratoryForm1Annex2GactivityBinding
import com.idsr_project.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Laboratory_Form_1_Annex2G_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLaboratoryForm1Annex2GactivityBinding
    private val calendar = Calendar.getInstance()
    private val repository by lazy { OfflineRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLaboratoryForm1Annex2GactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()
        setUpFieldListeners()
        setupDatePickers()
        autoFillSubmittedBy()

        binding.btnBackAnnex2G1.setOnClickListener {
            finish()
        }

        binding.btnLabHW1.setOnClickListener {
            if (validateLabForm1()) {
                submitSpecimenForm()
            }
        }
    }

    // ── added: store who submitted for status screen later ─────────────────────
    private fun autoFillSubmittedBy() {
        // SessionManager tracks the submitter — no UI field needed
        // just ensuring it's available when we build the entity in repository
    }

    private fun setupDropdowns() {
        val specimenTypes = resources.getStringArray(R.array.SpecimenType)
        val specimenAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            specimenTypes
        )
        binding.spinnerSpecimen.setAdapter(specimenAdapter)

        val genderOptions = arrayOf("Male", "Female")
        val genderAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        )
        binding.spinnerSex.setAdapter(genderAdapter)
    }

    private fun setupDatePickers() {
        binding.etDateSpecimenCollection.setOnClickListener {
            showDatePicker { binding.etDateSpecimenCollection.setText(it) }
        }
        binding.etDateSpecimenSentLab.setOnClickListener {
            showDatePicker { binding.etDateSpecimenSentLab.setText(it) }
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

    private fun setUpFieldListeners() {
        val fields = listOf(
            binding.tilDateSpecimenCollection to binding.etDateSpecimenCollection,
            binding.tilSuspectedDisease       to binding.etSuspectedDisease,
            binding.tilSpecienUniqueID        to binding.etSpecienUniqueID,
            binding.tilPatientNameLab         to binding.etPatientNameLab,
            binding.tilAge                    to binding.etAge,
            binding.tilDateSpecimenSentLab    to binding.etDateSpecimenSentLab,
            binding.tilPhoneNumber            to binding.etPhoneNumber
        )

        for ((layout, editText) in fields) {
            editText.addTextChangedListener {
                if (!it.isNullOrEmpty()) layout.error = null
            }
        }

        binding.spinnerSpecimen.addTextChangedListener {
            if (!it.isNullOrEmpty()) binding.tilSpecimenType.error = null
        }
        binding.spinnerSex.addTextChangedListener {
            if (!it.isNullOrEmpty()) binding.tilSex.error = null
        }
    }

    private fun validateLabForm1(): Boolean {
        var isValid = true

        if (binding.etDateSpecimenCollection.text.isNullOrEmpty()) {
            binding.tilDateSpecimenCollection.error = "Date of specimen collection is required"
            isValid = false
        } else binding.tilDateSpecimenCollection.error = null

        if (binding.etSuspectedDisease.text.isNullOrEmpty()) {
            binding.tilSuspectedDisease.error = "Suspected disease or condition is required"
            isValid = false
        } else binding.tilSuspectedDisease.error = null

        if (binding.spinnerSpecimen.text.isNullOrEmpty()) {
            binding.tilSpecimenType.error = "Specimen type is required"
            isValid = false
        } else binding.tilSpecimenType.error = null

        if (binding.etSpecienUniqueID.text.isNullOrEmpty()) {
            binding.tilSpecienUniqueID.error = "Specimen unique identifier is required"
            isValid = false
        } else binding.tilSpecienUniqueID.error = null

        if (binding.etPatientNameLab.text.isNullOrEmpty()) {
            binding.tilPatientNameLab.error = "Patient name is required"
            isValid = false
        } else binding.tilPatientNameLab.error = null

        if (binding.spinnerSex.text.isNullOrEmpty()) {
            binding.tilSex.error = "Sex is required"
            isValid = false
        } else binding.tilSex.error = null

        if (binding.etAge.text.isNullOrEmpty()) {
            binding.tilAge.error = "Patient age is required"
            isValid = false
        } else binding.tilAge.error = null

        if (binding.etDateSpecimenSentLab.text.isNullOrEmpty()) {
            binding.tilDateSpecimenSentLab.error = "Date specimen sent to laboratory is required"
            isValid = false
        } else binding.tilDateSpecimenSentLab.error = null

        if (binding.etPhoneNumber.text.isNullOrEmpty()) {
            binding.tilPhoneNumber.error = "Clinician phone number is required"
            isValid = false
        } else binding.tilPhoneNumber.error = null

        return isValid
    }

    private fun submitSpecimenForm() {
        binding.btnLabHW1.isEnabled = false
        binding.btnLabHW1.text = "Saving..."

        val reportForm = reportFormToLabWithSpecimen(
            dateSpecimenCollect  = binding.etDateSpecimenCollection.text.toString().trim(),
            suspectedDisease     = binding.etSuspectedDisease.text.toString().trim(),
            specimenType         = binding.spinnerSpecimen.text.toString().trim(),
            specimenUniqueID     = binding.etSpecienUniqueID.text.toString().trim(),
            patientNameLab       = binding.etPatientNameLab.text.toString().trim(),
            sex                  = binding.spinnerSex.text.toString().trim(),
            age                  = binding.etAge.text.toString().trim(),
            dateSpecimenSentLab  = binding.etDateSpecimenSentLab.text.toString().trim(),
            phoneNumber          = binding.etPhoneNumber.text.toString().trim(),
            emailClinician       = binding.etEmailClinician.text.toString().trim()
        )

        lifecycleScope.launch {
            val json = Gson().toJson(reportForm)

            when (val result = repository.submitReport("SPECIMEN", json)) {

                is SubmitResult.SyncedOnline -> {
                    Toast.makeText(
                        this@Laboratory_Form_1_Annex2G_Activity,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is SubmitResult.SavedOffline -> {
                    Toast.makeText(
                        this@Laboratory_Form_1_Annex2G_Activity,
                        "Report saved. Will sync automatically when online.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is SubmitResult.Error -> {
                    Toast.makeText(
                        this@Laboratory_Form_1_Annex2G_Activity,
                        "Error: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnLabHW1.isEnabled = true
                    binding.btnLabHW1.text = "Submit Form"
                    return@launch
                }
            }

            startActivity(Intent(this@Laboratory_Form_1_Annex2G_Activity, Success_Activity::class.java))
            finish()
        }
    }
}