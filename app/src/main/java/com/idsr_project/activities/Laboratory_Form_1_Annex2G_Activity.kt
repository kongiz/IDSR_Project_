package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.idsr_project.R
import com.idsr_project.Model.reportFormToLabWithSpecimen
import com.idsr_project.data.repository.OfflineRepository
import com.idsr_project.data.repository.SubmitResult
import com.idsr_project.databinding.ActivityLaboratoryForm1Annex2GactivityBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class Laboratory_Form_1_Annex2G_Activity : BaseActivity() {

    private lateinit var binding: ActivityLaboratoryForm1Annex2GactivityBinding
    private val calendar = Calendar.getInstance()
    private val repository by lazy { OfflineRepository(this) }
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLaboratoryForm1Annex2GactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()
        setUpFieldListeners()
        setupDatePickers()
        handleBackPress()

        generateSpecimenID()

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Laboratory_Form_1_Annex2G_Activity")

        binding.btnBackAnnex2G1.setOnClickListener { showExitWarning() }

        binding.btnLabHW1.setOnClickListener {
            if (validateLabForm1()) {
                submitSpecimenForm()
            }
        }
    }

    private fun generateSpecimenID() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val randomNum = (1000..9999).random()
        // Format: SPEC-2026-8492
        val generatedID = "SPEC-$year-$randomNum"
        binding.etSpecienUniqueID.setText(generatedID)
    }

    private fun setupDatePickers() {
        binding.etDateSpecimenCollection.setOnClickListener {
            showDatePicker(null) { date ->
                binding.etDateSpecimenCollection.setText(date)
                binding.etDateSpecimenSentLab.text = null
            }
        }

        binding.etDateSpecimenSentLab.setOnClickListener {
            val collectDateStr = binding.etDateSpecimenCollection.text.toString()
            if (collectDateStr.isEmpty()) {
                Toast.makeText(this, "Select collection date first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minDate = sdf.parse(collectDateStr)?.time
            showDatePicker(minDate) { date ->
                binding.etDateSpecimenSentLab.setText(date)
            }
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

    private fun validateLabForm1(): Boolean {
        var isValid = true

        val requiredFields = listOf(
            binding.etDateSpecimenCollection to binding.tilDateSpecimenCollection,
            binding.etSuspectedDisease to binding.tilSuspectedDisease,
            binding.etPatientNameLab to binding.tilPatientNameLab,
            binding.etAge to binding.tilAge,
            binding.etDateSpecimenSentLab to binding.tilDateSpecimenSentLab,
            binding.etPhoneNumber to binding.tilPhoneNumber
        )

        for ((et, til) in requiredFields) {
            if (et.text.isNullOrEmpty()) {
                til.error = "This field is required"
                isValid = false
            }
        }

        val age = binding.etAge.text.toString().toIntOrNull() ?: 0
        if (age > 120) {
            binding.tilAge.error = "Please enter a valid age"
            isValid = false
        }

        return isValid
    }

    private fun handleBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { showExitWarning() }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun showExitWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Form?")
            .setMessage("Are you sure? You will lose the entered specimen details.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep Editing", null)
            .show()
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